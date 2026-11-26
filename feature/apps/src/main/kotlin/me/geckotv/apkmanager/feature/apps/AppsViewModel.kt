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
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
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
        private val appInstaller: AppInstaller,
        private val deviceInfo: DeviceInfo,
    ) : ViewModel() {
        private val isRefreshing = MutableStateFlow(false)
        private val messageChannel = Channel<AppsMessage>(Channel.BUFFERED)
        private var refreshJob: Job? = null

        /**
         * Whether the running install of an app replaces an installed version, by app ID, to tell updates
         * from first installs. Filled when an install is first seen, whoever started it.
         */
        private val installsAreUpdates = mutableMapOf<Long, Boolean>()

        /** One-off messages for the user, e.g. why an update check failed. */
        val messages: Flow<AppsMessage> = messageChannel.receiveAsFlow()

        val uiState: StateFlow<AppsUiState> =
            combine(
                trackedAppsRepository.observeTrackedApps(),
                appInstaller.installs,
                isRefreshing,
            ) { apps, installs, refreshing ->
                AppsUiState.Ready(
                    groups = groupApps(apps.map { it.toItem(installs, deviceInfo.supportedAbis) }),
                    isRefreshing = refreshing,
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
            viewModelScope.launch {
                appInstaller.installs.collect { installs ->
                    installs.forEach { (appId, state) ->
                        if (state.isActive) onInstallRunning(appId) else onInstallFinished(appId, state)
                    }
                }
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

        /**
         * Installs the latest release of app [appId], or updates the app to it. While the install waits
         * for the user, shows the system confirmation again.
         */
        fun install(appId: Long) {
            if (appInstaller.installs.value[appId] is InstallState.WaitingForUser) {
                appInstaller.showConfirmationAgain(appId)
                return
            }
            viewModelScope.launch { findApp(appId)?.let(appInstaller::install) }
        }

        /** Updates every app with an update available that is not being installed already. */
        fun updateAll() {
            viewModelScope.launch {
                val installs = appInstaller.installs.value
                trackedAppsRepository
                    .observeTrackedApps()
                    .first()
                    .filter { it.updateStatus == UpdateStatus.UpdateAvailable && installs[it.id]?.isActive != true }
                    .forEach(appInstaller::install)
            }
        }

        private suspend fun findApp(appId: Long): TrackedApp? =
            trackedAppsRepository.observeTrackedApps().first().firstOrNull { it.id == appId }

        /** Remembers whether the install of app [appId] that just started replaces an installed version. */
        private suspend fun onInstallRunning(appId: Long) {
            if (appId !in installsAreUpdates) {
                installsAreUpdates[appId] = findApp(appId)?.updateStatus == UpdateStatus.UpdateAvailable
            }
        }

        /** Tells the user how the install of app [appId] ended, unless they cancelled it, and forgets it. */
        private suspend fun onInstallFinished(
            appId: Long,
            state: InstallState,
        ) {
            val wasUpdate = installsAreUpdates.remove(appId) == true
            appInstaller.dismiss(appId)
            val app = findApp(appId)
            val message =
                when {
                    state is InstallState.Succeeded && app != null -> {
                        if (wasUpdate) {
                            AppsMessage.Updated(app.displayName, state.versionName)
                        } else {
                            AppsMessage.Installed(app.displayName)
                        }
                    }

                    state is InstallState.Failed && state.failure != InstallFailure.Cancelled -> {
                        AppsMessage.InstallFailed(state.failure, app?.latestRelease?.tagName)
                    }

                    else -> {
                        null
                    }
                }
            message?.let { messageChannel.send(it) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/** Groups tracked apps by what the user can do next, each group sorted by name. Empty groups are left out. */
internal fun groupApps(items: List<AppItem>): List<AppGroup> {
    val sorted = items.sortedBy { it.app.displayName.lowercase() }
    return AppGroupKind.entries
        .map { kind -> AppGroup(kind, sorted.filter { AppGroupKind.of(it.app.updateStatus) == kind }) }
        .filter { it.items.isNotEmpty() }
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
     */
    data class Ready(
        val groups: List<AppGroup>,
        val isRefreshing: Boolean,
    ) : AppsUiState
}

/** Apps of one [kind], sorted by name. */
data class AppGroup(
    val kind: AppGroupKind,
    val items: List<AppItem>,
) {
    /** Whether the group offers to update all its apps: it has several, and some are not being installed yet. */
    val canUpdateAll: Boolean
        get() =
            kind == AppGroupKind.UpdatesAvailable &&
                items.size >= MIN_APPS_TO_UPDATE_ALL &&
                items.any { it.install == null }

    private companion object {
        const val MIN_APPS_TO_UPDATE_ALL = 2
    }
}

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

/** A one-off message for the user, e.g. about an update check or an install. */
sealed interface AppsMessage {
    data class CheckFailed(
        val count: Int,
    ) : AppsMessage

    data class RateLimited(
        val retryAt: Instant?,
    ) : AppsMessage

    data object Offline : AppsMessage

    data object Unauthorized : AppsMessage

    /** App [name] was installed. */
    data class Installed(
        val name: String,
    ) : AppsMessage

    /** App [name] was updated to [version]. */
    data class Updated(
        val name: String,
        val version: String?,
    ) : AppsMessage

    /** An install failed; [tag] is the release it tried to install, if known. */
    data class InstallFailed(
        val failure: InstallFailure,
        val tag: String?,
    ) : AppsMessage
}
