package me.geckotv.apkmanager.feature.apps

import me.geckotv.apkmanager.core.model.ApkAssetCandidate
import me.geckotv.apkmanager.core.model.ApkAssets
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceError

/**
 * @property input what the user typed: `owner/repo` or a GitHub URL.
 * @property inputError why [input] cannot be looked up.
 */
data class AddAppUiState(
    val input: String = "",
    val inputError: AddAppInputError? = null,
    val step: AddAppStep = AddAppStep.Input,
)

enum class AddAppInputError {
    /** The input is not a GitHub repository. */
    Invalid,

    /** The repository is tracked already. */
    AlreadyTracked,
}

sealed interface AddAppStep {
    /** The user enters a repository. */
    data object Input : AddAppStep

    /** The repository is being looked up. */
    data object Loading : AddAppStep

    data class Failed(
        val error: SourceError,
    ) : AddAppStep

    /**
     * The repository was found; the user reviews it.
     *
     * @property supportedAbis the device's ABIs, most preferred first.
     * @property chosenAssetName the APK the user picked explicitly, `null` if they did not.
     * @property packageName the installed app to link, `null` if the app is not installed.
     * @property isPickingPackage the list of installed apps is shown.
     * @property release the latest release, depending on [includePrereleases]; `null` if there is none.
     * @property candidates the APKs of [release], best first.
     * @property selectedAsset the APK that would be installed, `null` if no APK fits this device.
     */
    data class Preview(
        val preview: RepositoryPreview,
        val installedApps: List<InstalledApp>,
        val supportedAbis: List<String>,
        val includePrereleases: Boolean = false,
        val chosenAssetName: String? = null,
        val packageName: String? = null,
        val isPickingPackage: Boolean = false,
        val isAdding: Boolean = false,
    ) : AddAppStep {
        val release: Release? = preview.latestRelease(includePrereleases)
        val candidates: List<ApkAssetCandidate> = ApkAssets.candidates(release?.assets.orEmpty(), supportedAbis)

        /** Whether the user has to choose between several APKs. */
        val hasAssetChoice: Boolean = candidates.count { it.isCompatible } > 1

        val selectedAsset: ReleaseAsset? =
            candidates.firstOrNull { it.isCompatible && it.asset.name == chosenAssetName }?.asset
                ?: ApkAssets.select(release?.assets.orEmpty(), supportedAbis, chosenAssetName)

        /** The installed app that is linked, if [packageName] is installed. */
        val installedApp: InstalledApp?
            get() = installedApps.firstOrNull { it.packageName == packageName }

        /** Name of the APK to remember: only if the user picked one among several. */
        val preferredAssetName: String?
            get() = selectedAsset?.name?.takeIf { hasAssetChoice && chosenAssetName != null }

        /** Name shown after adding: the installed app's label, or the repository name. */
        val displayName: String
            get() = installedApp?.label ?: preview.repository.name
    }
}

sealed interface AddAppEvent {
    /** The app [name] is tracked now. */
    data class Added(
        val name: String,
    ) : AddAppEvent
}
