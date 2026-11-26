package me.geckotv.apkmanager.feature.apps.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.feature.apps.toItem
import me.geckotv.apkmanager.feature.apps.toMessage

/**
 * The details of the tracked app [appId]: its latest release, its release history and the actions
 * to install it, to skip a release, to change its settings and options and to stop tracking it.
 *
 * It has one function per action of the screen; splitting it would only scatter them.
 */
@Suppress("TooManyFunctions")
@HiltViewModel(assistedFactory = AppDetailsViewModel.Factory::class)
class AppDetailsViewModel
    @AssistedInject
    constructor(
        @Assisted private val appId: Long,
        private val trackedAppsRepository: TrackedAppsRepository,
        private val installedAppsRepository: InstalledAppsRepository,
        private val appInstaller: AppInstaller,
        private val deviceInfo: DeviceInfo,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(appId: Long): AppDetailsViewModel
        }

        private val localState = MutableStateFlow(LocalState())
        private val eventChannel = Channel<AppDetailsEvent>(Channel.BUFFERED)
        private var historyJob: Job? = null

        /** One-off events, e.g. that the app is gone and the screen should close. */
        val events: Flow<AppDetailsEvent> = eventChannel.receiveAsFlow()

        val uiState: StateFlow<AppDetailsUiState> =
            combine(
                trackedAppsRepository.observeTrackedApp(appId),
                appInstaller.installs,
                localState,
            ) { app, installs, local ->
                if (app == null) {
                    AppDetailsUiState.Loading
                } else {
                    AppDetailsUiState.Ready(
                        item = app.toItem(installs, deviceInfo.supportedAbis),
                        history = local.history,
                        installedApps = local.installedApps,
                        isConfirmingRemove = local.isConfirmingRemove,
                        expandedReleaseId = local.expandedReleaseId,
                        optionsSheet = local.optionsSheet,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AppDetailsUiState.Loading,
            )

        init {
            retryHistory()
            // The app is gone when the user removes it here, but also if it is removed elsewhere.
            viewModelScope.launch {
                trackedAppsRepository.observeTrackedApp(appId).first { it == null }
                eventChannel.send(AppDetailsEvent.Closed)
            }
        }

        /** Expands the release [releaseId] in the history, or collapses it if it is expanded already. */
        fun toggleRelease(releaseId: Long) {
            localState.update { it.copy(expandedReleaseId = releaseId.takeUnless { id -> id == it.expandedReleaseId }) }
        }

        /**
         * Installs the latest release, or updates the app to it. While the install waits for the user,
         * shows the system confirmation again.
         */
        fun install() {
            if (appInstaller.installs.value[appId] is InstallState.WaitingForUser) {
                appInstaller.showConfirmationAgain(appId)
                return
            }
            viewModelScope.launch {
                trackedAppsRepository.observeTrackedApp(appId).first()?.let(appInstaller::install)
            }
        }

        /** Cancels the running install. */
        fun cancelInstall() {
            appInstaller.cancel(appId)
        }

        fun setIncludePrereleases(includePrereleases: Boolean) {
            viewModelScope.launch {
                trackedAppsRepository
                    .setIncludePrereleases(appId, includePrereleases)
                    .toMessage()
                    ?.let { eventChannel.send(AppDetailsEvent.ShowMessage(it)) }
            }
        }

        /** Skips the latest release, so it does not count as an update. */
        fun skipLatestRelease() {
            viewModelScope.launch {
                val releaseId =
                    trackedAppsRepository
                        .observeTrackedApp(appId)
                        .first()
                        ?.latestRelease
                        ?.id
                if (releaseId != null) trackedAppsRepository.setSkippedRelease(appId, releaseId)
            }
        }

        /** Stops skipping the release that was skipped. */
        fun undoSkip() {
            viewModelScope.launch { trackedAppsRepository.setSkippedRelease(appId, null) }
        }

        /** Shows the sheet to edit which APKs of a release count. */
        fun showAssetFilter() {
            localState.update { it.copy(optionsSheet = OptionsSheet.AssetFilterSheet(deviceInfo.supportedAbis)) }
        }

        /**
         * Saves the APK filter: assets must match [include] and must not match [exclude]; a blank pattern
         * does not filter. Nothing is saved if a pattern is not a valid regular expression.
         */
        fun saveAssetFilter(
            include: String,
            exclude: String,
        ) {
            val includePattern = include.trim().ifEmpty { null }
            val excludePattern = exclude.trim().ifEmpty { null }
            if (includePattern.isInvalidPattern() || excludePattern.isInvalidPattern()) return
            val filter = AssetFilter(includePattern, excludePattern)
            dismissOptionsSheet()
            viewModelScope.launch { trackedAppsRepository.setAssetFilter(appId, filter) }
        }

        /** Shows the sheet to edit how the version is read from a release tag. */
        fun showVersionPattern() {
            localState.update { it.copy(optionsSheet = OptionsSheet.VersionPatternSheet) }
        }

        /**
         * Saves the pattern that extracts the version from release tags; a blank [pattern] reads the
         * tag as it is. Nothing is saved if the pattern is not a valid regular expression.
         */
        fun saveVersionPattern(pattern: String) {
            val trimmed = pattern.trim().ifEmpty { null }
            if (trimmed.isInvalidPattern()) return
            dismissOptionsSheet()
            viewModelScope.launch { trackedAppsRepository.setVersionPattern(appId, trimmed) }
        }

        /** Closes the sheet to edit an advanced option without saving. */
        fun dismissOptionsSheet() {
            localState.update { it.copy(optionsSheet = null) }
        }

        /** Shows the list of installed apps to link to the app. */
        fun showPackagePicker() {
            viewModelScope.launch {
                val installedApps = installedAppsRepository.installedApps()
                localState.update { it.copy(installedApps = installedApps) }
            }
        }

        fun closePackagePicker() {
            localState.update { it.copy(installedApps = null) }
        }

        /** Links the app to the installed [packageName], or unlinks it with `null`. */
        fun setPackageName(packageName: String?) {
            viewModelScope.launch {
                trackedAppsRepository.setPackageName(appId, packageName)
                closePackagePicker()
            }
        }

        /** Asks the user to confirm that the app is removed. */
        fun showRemoveConfirmation() {
            localState.update { it.copy(isConfirmingRemove = true) }
        }

        fun dismissRemoveConfirmation() {
            localState.update { it.copy(isConfirmingRemove = false) }
        }

        /** Stops tracking the app; the app stays installed. [events] then reports that the screen should close. */
        fun remove() {
            localState.update { it.copy(isConfirmingRemove = false) }
            viewModelScope.launch { trackedAppsRepository.removeApp(appId) }
        }

        /** Loads the release history, again if it was loaded before, e.g. after it failed. */
        fun retryHistory() {
            historyJob?.cancel()
            historyJob =
                viewModelScope.launch {
                    localState.update { it.copy(history = ReleaseHistoryState.Loading) }
                    val history =
                        when (val result = trackedAppsRepository.releaseHistory(appId)) {
                            is SourceResult.Success -> ReleaseHistoryState.Loaded(result.data)
                            is SourceResult.Failure -> ReleaseHistoryState.Failed(result.error)
                        }
                    localState.update { it.copy(history = history) }
                }
        }

        /** What the screen shows besides the app and its install. */
        private data class LocalState(
            val history: ReleaseHistoryState = ReleaseHistoryState.Loading,
            val installedApps: List<InstalledApp>? = null,
            val isConfirmingRemove: Boolean = false,
            val expandedReleaseId: Long? = null,
            val optionsSheet: OptionsSheet? = null,
        )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
