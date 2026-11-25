package me.geckotv.apkmanager.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class AppsViewModel
    @Inject
    constructor(
        private val trackedAppsRepository: TrackedAppsRepository,
        private val installedAppsRepository: InstalledAppsRepository,
    ) : ViewModel() {
        private val isRefreshing = MutableStateFlow(false)
        private val selection = MutableStateFlow<Selection?>(null)
        private val messageChannel = Channel<AppsMessage>(Channel.BUFFERED)
        private var refreshJob: Job? = null

        /** One-off messages for the user, e.g. why an update check failed. */
        val messages: Flow<AppsMessage> = messageChannel.receiveAsFlow()

        val uiState: StateFlow<AppsUiState> =
            combine(
                trackedAppsRepository.observeTrackedApps(),
                isRefreshing,
                selection,
            ) { apps, refreshing, selected ->
                AppsUiState.Ready(
                    groups = groupApps(apps),
                    isRefreshing = refreshing,
                    actions =
                        selected?.let {
                            apps.firstOrNull { app -> app.id == selected.appId }?.let { app ->
                                AppActionsUiState(app = app, installedApps = selected.installedApps)
                            }
                        },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AppsUiState.Loading,
            )

        init {
            viewModelScope.launch {
                if (trackedAppsRepository.observeTrackedApps().first().isNotEmpty()) refresh()
            }
        }

        /** Checks all apps for updates. Does nothing while a check is running. */
        fun refresh() {
            if (refreshJob?.isActive == true) return
            refreshJob =
                viewModelScope.launch {
                    isRefreshing.value = true
                    try {
                        trackedAppsRepository.checkForUpdates().toMessage()?.let { messageChannel.send(it) }
                    } finally {
                        isRefreshing.value = false
                    }
                }
        }

        /** Opens the actions of app [id]. */
        fun selectApp(id: Long) {
            selection.value = Selection(appId = id, installedApps = null)
        }

        fun dismissApp() {
            selection.value = null
        }

        /** Shows the list of installed apps to link to the selected app. */
        fun showPackagePicker() {
            val appId = selection.value?.appId ?: return
            viewModelScope.launch {
                val installedApps = installedAppsRepository.installedApps()
                selection.update { current ->
                    current?.takeIf { it.appId == appId }?.copy(installedApps = installedApps)
                }
            }
        }

        fun closePackagePicker() {
            selection.update { it?.copy(installedApps = null) }
        }

        /** Links the selected app to the installed [packageName], or unlinks it with `null`. */
        fun setPackageName(packageName: String?) {
            val appId = selection.value?.appId ?: return
            viewModelScope.launch {
                trackedAppsRepository.setPackageName(appId, packageName)
                closePackagePicker()
            }
        }

        fun setIncludePrereleases(includePrereleases: Boolean) {
            val appId = selection.value?.appId ?: return
            viewModelScope.launch {
                trackedAppsRepository
                    .setIncludePrereleases(appId, includePrereleases)
                    .toMessage()
                    ?.let { messageChannel.send(it) }
            }
        }

        /** Stops tracking the selected app and closes its actions. */
        fun removeApp() {
            val appId = selection.value?.appId ?: return
            selection.value = null
            viewModelScope.launch { trackedAppsRepository.removeApp(appId) }
        }

        private data class Selection(
            val appId: Long,
            /** The installed apps to pick from; `null` while the actions are shown. */
            val installedApps: List<InstalledApp>?,
        )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/** Groups tracked apps by what the user can do next, each group sorted by name. Empty groups are left out. */
internal fun groupApps(apps: List<TrackedApp>): List<AppGroup> {
    val sorted = apps.sortedBy { it.displayName.lowercase() }
    return AppGroupKind.entries
        .map { kind -> AppGroup(kind, sorted.filter { AppGroupKind.of(it.updateStatus) == kind }) }
        .filter { it.apps.isNotEmpty() }
}

/** Picks the message that explains best why an update check failed; `null` if it did not fail. */
internal fun UpdateCheckResult.toMessage(): AppsMessage? {
    val errors = failures.values
    return when {
        errors.isEmpty() -> {
            null
        }

        errors.any { it == SourceError.Offline } -> {
            AppsMessage.Offline
        }

        errors.any { it is SourceError.RateLimited } -> {
            AppsMessage.RateLimited(
                errors.filterIsInstance<SourceError.RateLimited>().firstNotNullOfOrNull { it.retryAt },
            )
        }

        errors.any { it == SourceError.Unauthorized } -> {
            AppsMessage.Unauthorized
        }

        else -> {
            AppsMessage.CheckFailed(errors.size)
        }
    }
}

sealed interface AppsUiState {
    data object Loading : AppsUiState

    /**
     * @property groups the tracked apps; empty if no app is tracked.
     * @property isRefreshing an update check is running.
     * @property actions the app whose actions sheet is open.
     */
    data class Ready(
        val groups: List<AppGroup>,
        val isRefreshing: Boolean,
        val actions: AppActionsUiState?,
    ) : AppsUiState
}

/** Apps of one [kind], sorted by name. */
data class AppGroup(
    val kind: AppGroupKind,
    val apps: List<TrackedApp>,
)

enum class AppGroupKind {
    UpdatesAvailable,
    UpToDate,
    NotInstalled,
    ;

    companion object {
        fun of(status: UpdateStatus): AppGroupKind =
            when (status) {
                UpdateStatus.UpdateAvailable -> UpdatesAvailable
                UpdateStatus.UpToDate, UpdateStatus.Unknown, UpdateStatus.NoRelease -> UpToDate
                UpdateStatus.NotInstalled -> NotInstalled
            }
    }
}

/**
 * The open actions sheet of [app]. While [installedApps] is not `null`, the sheet shows the list
 * of installed apps to link instead of the actions.
 */
data class AppActionsUiState(
    val app: TrackedApp,
    val installedApps: List<InstalledApp>?,
)

/** A one-off message about an update check. */
sealed interface AppsMessage {
    data class CheckFailed(
        val count: Int,
    ) : AppsMessage

    data class RateLimited(
        val retryAt: Instant?,
    ) : AppsMessage

    data object Offline : AppsMessage

    data object Unauthorized : AppsMessage
}
