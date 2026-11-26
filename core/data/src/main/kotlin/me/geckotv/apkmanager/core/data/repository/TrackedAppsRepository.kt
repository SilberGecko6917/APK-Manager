package me.geckotv.apkmanager.core.data.repository

import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy

/**
 * The apps the user tracks and their releases. It has one function per operation on tracked apps;
 * splitting it would only scatter them.
 */
@Suppress("TooManyFunctions")
interface TrackedAppsRepository {
    /** All tracked apps with their latest release and installed version. Updates on package changes. */
    fun observeTrackedApps(): Flow<List<TrackedApp>>

    /** One tracked app, or `null` if it does not exist (any more). */
    fun observeTrackedApp(id: Long): Flow<TrackedApp?>

    /** The ID of the app tracking [repository] (ignoring case), or `null`. */
    suspend fun findTrackedApp(repository: GitHubRepository): Long?

    /**
     * The newest published releases of app [id] (pre-releases included), fetched from the source; they
     * are not stored. Fails with [SourceError.NotFound] if the app does not exist.
     */
    suspend fun releaseHistory(id: Long): SourceResult<List<Release>>

    /** Fetches a repository and its newest releases so the user can review it before adding it. */
    suspend fun lookUpRepository(repository: GitHubRepository): SourceResult<RepositoryPreview>

    /**
     * Starts tracking the repository of [preview] and stores its latest release. Returns the new app's
     * ID, or the existing app's ID if the repository is already tracked.
     */
    suspend fun addApp(
        preview: RepositoryPreview,
        options: AddAppOptions,
    ): Long

    /**
     * Starts tracking [repository] without looking it up first; its latest release is fetched by the
     * next update check. Returns the app's ID, or the existing app's ID if it is tracked already.
     */
    suspend fun trackRepository(
        repository: GitHubRepository,
        packageName: String,
        displayName: String,
        includePrereleases: Boolean,
    ): Long

    suspend fun removeApp(id: Long)

    /** Links app [id] to an installed package, or unlinks it with `null`. */
    suspend fun setPackageName(
        id: Long,
        packageName: String?,
    )

    /** Remembers which asset to install when a release has several APKs. */
    suspend fun setPreferredAssetName(
        id: Long,
        assetName: String?,
    )

    /** Sets which release assets count for app [id]. */
    suspend fun setAssetFilter(
        id: Long,
        filter: AssetFilter,
    )

    /** Sets the pattern that extracts the version from the release tags of app [id]; `null` removes it. */
    suspend fun setVersionPattern(
        id: Long,
        pattern: String?,
    )

    /** Remembers that APK Manager installed release [releaseId] of app [id] as [versionCode]. */
    suspend fun setInstalledRelease(
        id: Long,
        releaseId: Long,
        versionCode: Long,
    )

    /** Skips release [releaseId] of app [id] so it does not count as an update; `null` stops skipping. */
    suspend fun setSkippedRelease(
        id: Long,
        releaseId: Long?,
    )

    /** Sets what to do with updates of app [id]; `null` follows the default in the settings. */
    suspend fun setUpdatePolicy(
        id: Long,
        policy: UpdatePolicy?,
    )

    /** Remembers that APK Manager tried to install release [releaseId] of app [id] automatically. */
    suspend fun setAutoUpdateRelease(
        id: Long,
        releaseId: Long,
    )

    /** Remembers that the user was notified about release [releaseId] of app [id]. */
    suspend fun setNotifiedRelease(
        id: Long,
        releaseId: Long,
    )

    /** Changes whether pre-releases count as updates and checks the app again. */
    suspend fun setIncludePrereleases(
        id: Long,
        includePrereleases: Boolean,
    ): UpdateCheckResult

    /** Checks all tracked apps for new releases. */
    suspend fun checkForUpdates(): UpdateCheckResult
}

/** Settings for a newly added app. */
data class AddAppOptions(
    /** The installed app built from the repository, if any. */
    val packageName: String?,
    val includePrereleases: Boolean,
    /** The asset the user picked, if the latest release has several APKs. */
    val preferredAssetName: String?,
)

/** Outcome of an update check. */
data class UpdateCheckResult(
    /** Number of apps that were checked successfully. */
    val checkedCount: Int,
    /** Apps that could not be checked, by app ID. */
    val failures: Map<Long, SourceError>,
) {
    val isSuccess: Boolean get() = failures.isEmpty()
}
