package me.geckotv.apkmanager.feature.apps

import android.content.Intent
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
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.ApkAssets
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class AppsViewModel
    @Inject
    constructor(
        private val trackedAppsRepository: TrackedAppsRepository,
        private val installedAppsRepository: InstalledAppsRepository,
        private val appInstaller: AppInstaller,
        private val deviceInfo: DeviceInfo,
    ) : ViewModel() {
        private val isRefreshing = MutableStateFlow(false)
        private val selection = MutableStateFlow<Selection?>(null)
        private val messageChannel = Channel<AppsMessage>(Channel.BUFFERED)
        private var refreshJob: Job? = null

        /** Apps whose running install replaces an installed version, to tell updates from first installs. */
        private val updatingAppIds = mutableSetOf<Long>()

        /** One-off messages for the user, e.g. why an update check failed. */
        val messages: Flow<AppsMessage> = messageChannel.receiveAsFlow()

        val uiState: StateFlow<AppsUiState> =
            combine(
                trackedAppsRepository.observeTrackedApps(),
                appInstaller.installs,
                isRefreshing,
                selection,
            ) { apps, installs, refreshing, selected ->
                val items = apps.map { it.toItem(installs) }
                AppsUiState.Ready(
                    groups = groupApps(items),
                    isRefreshing = refreshing,
                    actions =
                        selected?.let {
                            items.firstOrNull { item -> item.id == selected.appId }?.let { item ->
                                AppActionsUiState(item = item, installedApps = selected.installedApps)
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
            viewModelScope.launch {
                appInstaller.installs.collect { installs ->
                    installs.forEach { (appId, state) -> if (!state.isActive) onInstallFinished(appId, state) }
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

        /**
         * Installs the latest release of app [appId], or updates the app to it. While the install waits
         * for the user, shows the system confirmation again.
         */
        fun install(appId: Long) {
            if (appInstaller.installs.value[appId] is InstallState.WaitingForUser) {
                appInstaller.showConfirmationAgain(appId)
                return
            }
            viewModelScope.launch {
                trackedAppsRepository
                    .observeTrackedApps()
                    .first()
                    .firstOrNull { it.id == appId }
                    ?.let(::startInstall)
            }
        }

        /** Updates every app with an update available that is not being installed already. */
        fun updateAll() {
            viewModelScope.launch {
                val installs = appInstaller.installs.value
                trackedAppsRepository
                    .observeTrackedApps()
                    .first()
                    .filter { it.updateStatus == UpdateStatus.UpdateAvailable && installs[it.id]?.isActive != true }
                    .forEach(::startInstall)
            }
        }

        /** Cancels the running install of app [appId]. */
        fun cancelInstall(appId: Long) {
            updatingAppIds -= appId
            appInstaller.cancel(appId)
        }

        /** The system confirmation of the install of app [appId] is on screen. */
        fun onConfirmationShown(appId: Long) {
            appInstaller.onConfirmationShown(appId)
        }

        /** The system confirmation of the install of app [appId] could not be shown. */
        fun onConfirmationFailed(appId: Long) {
            cancelInstall(appId)
            messageChannel.trySend(AppsMessage.InstallFailed(InstallFailure.SystemError(message = null), tag = null))
        }

        private fun startInstall(app: TrackedApp) {
            if (app.updateStatus == UpdateStatus.UpdateAvailable) updatingAppIds += app.id
            appInstaller.install(app)
        }

        /** Tells the user how the install of app [appId] ended, unless they cancelled it, and forgets it. */
        private suspend fun onInstallFinished(
            appId: Long,
            state: InstallState,
        ) {
            val wasUpdate = updatingAppIds.remove(appId)
            appInstaller.dismiss(appId)
            val app = trackedAppsRepository.observeTrackedApps().first().firstOrNull { it.id == appId }
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

        /** The list item of this app, with its running install and whether the latest release fits this device. */
        private fun TrackedApp.toItem(installs: Map<Long, InstallState>): AppItem =
            AppItem(
                app = this,
                install = installs[id]?.takeIf { it.isActive },
                hasCompatibleApk =
                    latestRelease?.let { ApkAssets.select(it.assets, deviceInfo.supportedAbis, preferredAssetName) } !=
                        null,
            )

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
     * @property actions the app whose actions sheet is open.
     */
    data class Ready(
        val groups: List<AppGroup>,
        val isRefreshing: Boolean,
        val actions: AppActionsUiState?,
    ) : AppsUiState {
        /** The first install that waits for the user to confirm it in the system dialog. */
        val pendingConfirmation: PendingConfirmation?
            get() =
                groups
                    .asSequence()
                    .flatMap { it.items }
                    .firstNotNullOfOrNull { item ->
                        (item.install as? InstallState.WaitingForUser)
                            ?.takeUnless { it.isShown }
                            ?.let { PendingConfirmation(item.id, it.intent) }
                    }
    }
}

/** The system dialog to launch so the user confirms the install of app [appId]. */
data class PendingConfirmation(
    val appId: Long,
    val intent: Intent,
)

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

/**
 * A tracked [app] in the list.
 *
 * @property install the running install of the app, `null` if none is running.
 * @property hasCompatibleApk whether the latest release has an APK for this device.
 */
data class AppItem(
    val app: TrackedApp,
    val install: InstallState? = null,
    val hasCompatibleApk: Boolean = false,
) {
    val id: Long get() = app.id

    /** The install button to offer, `null` if none fits or an install is running. */
    val installAction: InstallAction?
        get() =
            when {
                install is InstallState.WaitingForUser && install.isShown -> InstallAction.Confirm
                install != null -> null
                app.updateStatus == UpdateStatus.UpdateAvailable -> InstallAction.Update
                app.updateStatus == UpdateStatus.NotInstalled && hasCompatibleApk -> InstallAction.Install
                else -> null
            }
}

/** What installing the latest release does to an app. */
enum class InstallAction {
    Install,
    Update,

    /** Show the system confirmation of a running install again. */
    Confirm,
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

/**
 * The open actions sheet of [item]. While [installedApps] is not `null`, the sheet shows the list
 * of installed apps to link instead of the actions.
 */
data class AppActionsUiState(
    val item: AppItem,
    val installedApps: List<InstalledApp>?,
) {
    val app: TrackedApp get() = item.app
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
