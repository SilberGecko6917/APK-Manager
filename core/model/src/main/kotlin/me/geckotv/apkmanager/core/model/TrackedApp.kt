package me.geckotv.apkmanager.core.model

import java.time.Instant

/** An app whose GitHub releases APK Manager watches. */
data class TrackedApp(
    val id: Long,
    val repository: GitHubRepository,
    /** Name shown in the app list: the installed app's label, or the repository name. */
    val displayName: String,
    /** The repository description. */
    val description: String?,
    val ownerAvatarUrl: String?,
    val isPrivate: Boolean,
    /**
     * Package name of the Android app built from the repository. `null` until it is known, i.e. the
     * user picked the installed app or APK Manager installed the app.
     */
    val packageName: String?,
    /** Whether pre-releases count as updates. */
    val includePrereleases: Boolean,
    /**
     * Name of the asset the user picked when the release had several APKs. Used to pick the matching
     * asset of later releases, see [ApkAssets.select].
     */
    val preferredAssetName: String?,
    /** The newest release that counts for this app, `null` if none was found yet. */
    val latestRelease: Release?,
    /** The installed app, `null` if [packageName] is unknown or the app is not installed. */
    val installedApp: InstalledApp?,
    val addedAt: Instant,
    /** When the latest release was last checked successfully. */
    val lastCheckedAt: Instant?,
    /** GitHub ID of the release the user was last notified about, `null` if none. */
    val notifiedReleaseId: Long? = null,
) {
    /** Whether an update is available, see [UpdateStatus]. */
    val updateStatus: UpdateStatus
        get() = UpdateStatus.of(installedApp = installedApp, latestRelease = latestRelease)
}
