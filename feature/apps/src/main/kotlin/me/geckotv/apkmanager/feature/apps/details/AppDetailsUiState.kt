package me.geckotv.apkmanager.feature.apps.details

import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.feature.apps.AppItem
import me.geckotv.apkmanager.feature.apps.AppsMessage

sealed interface AppDetailsUiState {
    /** The app is not loaded yet. */
    data object Loading : AppDetailsUiState

    /**
     * @property item the app with its running install.
     * @property history the releases of the app, newest first.
     * @property installedApps the installed apps to pick from while the list is shown, `null` otherwise.
     * @property isConfirmingRemove whether the user is asked to confirm that the app is removed.
     * @property expandedReleaseId the release of the history whose notes are shown, if any.
     * @property optionsSheet the sheet to edit an advanced option that is shown, `null` if none is.
     */
    data class Ready(
        val item: AppItem,
        val history: ReleaseHistoryState,
        val installedApps: List<InstalledApp>?,
        val isConfirmingRemove: Boolean,
        val expandedReleaseId: Long?,
        val optionsSheet: OptionsSheet? = null,
    ) : AppDetailsUiState
}

/** A sheet to edit one of the advanced options of an app. */
sealed interface OptionsSheet {
    /**
     * The sheet for the APK filter.
     *
     * @property supportedAbis the ABIs of this device, to preview which APK would be installed.
     */
    data class AssetFilterSheet(
        val supportedAbis: List<String>,
    ) : OptionsSheet

    /** The sheet for the pattern that extracts the version from release tags. */
    data object VersionPatternSheet : OptionsSheet
}

/** The newest releases of an app, loaded from the source. */
sealed interface ReleaseHistoryState {
    data object Loading : ReleaseHistoryState

    data class Loaded(
        val releases: List<Release>,
    ) : ReleaseHistoryState

    data class Failed(
        val error: SourceError,
    ) : ReleaseHistoryState
}

/** A one-off event for the screen. */
sealed interface AppDetailsEvent {
    /** The app is not tracked any more, e.g. because the user removed it; the screen should close. */
    data object Closed : AppDetailsEvent

    /** Something the user should know, e.g. that the update check after a settings change failed. */
    data class ShowMessage(
        val message: AppsMessage,
    ) : AppDetailsEvent
}
