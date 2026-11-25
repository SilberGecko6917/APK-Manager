package me.geckotv.apkmanager.core.data.repository

import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.model.TrackedApp

/** The apps the user tracks and their releases. */
// One function per operation on tracked apps; splitting it would only scatter them.
@Suppress("TooManyFunctions")
interface TrackedAppsRepository {
    /** All tracked apps with their latest release and installed version. Updates on package changes. */
    fun observeTrackedApps(): Flow<List<TrackedApp>>

    /** One tracked app, or `null` if it does not exist (any more). */
    fun observeTrackedApp(id: Long): Flow<TrackedApp?>

    /** The ID of the app tracking [repository] (ignoring case), or `null`. */
    suspend fun findTrackedApp(repository: GitHubRepository): Long?

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
