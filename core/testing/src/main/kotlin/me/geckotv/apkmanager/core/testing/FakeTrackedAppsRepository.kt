package me.geckotv.apkmanager.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import me.geckotv.apkmanager.core.data.repository.AddAppOptions
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.InstalledRelease
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy
import java.time.Instant

/**
 * In-memory [TrackedAppsRepository] for tests. Put lookup results into [previews] (missing
 * repositories fail with [SourceError.NotFound]) and set [updateCheckResult] for update checks.
 */
class FakeTrackedAppsRepository(
    initialApps: List<TrackedApp> = emptyList(),
) : TrackedAppsRepository {
    private val apps = MutableStateFlow(initialApps)

    val previews = mutableMapOf<GitHubRepository, SourceResult<RepositoryPreview>>()

    /** Release histories by app ID; apps without an entry fail with [SourceError.NotFound]. */
    val releaseHistories = mutableMapOf<Long, SourceResult<List<Release>>>()
    var updateCheckResult = UpdateCheckResult(checkedCount = 0, failures = emptyMap())
    var updateCheckCount = 0
        private set
    val addedOptions = mutableListOf<AddAppOptions>()

    val trackedApps: List<TrackedApp> get() = apps.value

    fun setApps(apps: List<TrackedApp>) {
        this.apps.value = apps
    }

    override fun observeTrackedApps(): Flow<List<TrackedApp>> = apps

    override fun observeTrackedApp(id: Long): Flow<TrackedApp?> = apps.map { apps -> apps.firstOrNull { it.id == id } }

    override suspend fun findTrackedApp(repository: GitHubRepository): Long? =
        apps.value
            .firstOrNull { it.repository.fullName.equals(repository.fullName, ignoreCase = true) }
            ?.id

    override suspend fun releaseHistory(id: Long): SourceResult<List<Release>> =
        releaseHistories[id] ?: SourceResult.Failure(SourceError.NotFound)

    override suspend fun lookUpRepository(repository: GitHubRepository): SourceResult<RepositoryPreview> =
        previews.entries.firstOrNull { it.key.fullName.equals(repository.fullName, ignoreCase = true) }?.value
            ?: SourceResult.Failure(SourceError.NotFound)

    override suspend fun addApp(
        preview: RepositoryPreview,
        options: AddAppOptions,
    ): Long {
        findTrackedApp(preview.repository)?.let { return it }
        addedOptions += options
        val id = (apps.value.maxOfOrNull { it.id } ?: 0) + 1
        apps.value +=
            TrackedApp(
                id = id,
                repository = preview.repository,
                displayName = preview.repository.name,
                description = preview.description,
                ownerAvatarUrl = preview.ownerAvatarUrl,
                isPrivate = preview.isPrivate,
                packageName = options.packageName,
                includePrereleases = options.includePrereleases,
                preferredAssetName = options.preferredAssetName,
                latestRelease = preview.latestRelease(options.includePrereleases),
                installedApp = null,
                addedAt = Instant.EPOCH,
                lastCheckedAt = null,
            )
        return id
    }

    override suspend fun trackRepository(
        repository: GitHubRepository,
        packageName: String,
        displayName: String,
        includePrereleases: Boolean,
    ): Long =
        findTrackedApp(repository) ?: addApp(
            RepositoryPreview(
                repository = repository,
                description = null,
                ownerAvatarUrl = null,
                isPrivate = false,
                isArchived = false,
                stars = 0,
                releases = emptyList(),
            ),
            AddAppOptions(packageName, includePrereleases, preferredAssetName = null),
        ).also { id -> update(id) { it.copy(displayName = displayName) } }

    override suspend fun removeApp(id: Long) {
        apps.value = apps.value.filterNot { it.id == id }
    }

    override suspend fun setPackageName(
        id: Long,
        packageName: String?,
    ) {
        update(id) { it.copy(packageName = packageName) }
    }

    override suspend fun setPreferredAssetName(
        id: Long,
        assetName: String?,
    ) {
        update(id) { it.copy(preferredAssetName = assetName) }
    }

    override suspend fun setAssetFilter(
        id: Long,
        filter: AssetFilter,
    ) {
        update(id) { it.copy(assetFilter = filter) }
    }

    override suspend fun setVersionPattern(
        id: Long,
        pattern: String?,
    ) {
        update(id) { it.copy(versionPattern = pattern) }
    }

    override suspend fun setInstalledRelease(
        id: Long,
        releaseId: Long,
        versionCode: Long,
    ) {
        update(id) { it.copy(installedRelease = InstalledRelease(releaseId, versionCode), skippedReleaseId = null) }
    }

    override suspend fun setSkippedRelease(
        id: Long,
        releaseId: Long?,
    ) {
        update(id) { it.copy(skippedReleaseId = releaseId) }
    }

    override suspend fun setUpdatePolicy(
        id: Long,
        policy: UpdatePolicy?,
    ) {
        update(id) { it.copy(updatePolicy = policy) }
    }

    override suspend fun setAutoUpdateRelease(
        id: Long,
        releaseId: Long,
    ) {
        update(id) { it.copy(autoUpdateReleaseId = releaseId) }
    }

    override suspend fun setNotifiedRelease(
        id: Long,
        releaseId: Long,
    ) {
        update(id) { it.copy(notifiedReleaseId = releaseId) }
    }

    override suspend fun setIncludePrereleases(
        id: Long,
        includePrereleases: Boolean,
    ): UpdateCheckResult {
        update(id) { it.copy(includePrereleases = includePrereleases) }
        return updateCheckResult
    }

    override suspend fun checkForUpdates(): UpdateCheckResult {
        updateCheckCount++
        return updateCheckResult
    }

    private fun update(
        id: Long,
        transform: (TrackedApp) -> TrackedApp,
    ) {
        apps.value = apps.value.map { if (it.id == id) transform(it) else it }
    }
}
