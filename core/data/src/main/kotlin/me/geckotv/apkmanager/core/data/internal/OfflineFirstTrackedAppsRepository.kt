package me.geckotv.apkmanager.core.data.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.data.repository.AddAppOptions
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.database.dao.TrackedAppDao
import me.geckotv.apkmanager.core.database.model.TrackedAppEntity
import me.geckotv.apkmanager.core.database.model.TrackedAppWithRelease
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import java.time.Clock
import javax.inject.Inject

/** [TrackedAppsRepository] that keeps apps and their latest releases in the database. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class OfflineFirstTrackedAppsRepository
    @Inject
    constructor(
        private val dao: TrackedAppDao,
        private val network: GitHubNetworkDataSource,
        private val installedApps: InstalledAppsRepository,
        private val releaseChecker: ReleaseChecker,
        private val clock: Clock,
        @Dispatcher(ApkManagerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : TrackedAppsRepository {
        override fun observeTrackedApps(): Flow<List<TrackedApp>> =
            combine(dao.observeTrackedApps(), installedApps.packageChanges) { apps, _ -> apps }
                .mapLatest { apps -> apps.map { it.withInstalledApp() } }
                .flowOn(ioDispatcher)

        override fun observeTrackedApp(id: Long): Flow<TrackedApp?> =
            combine(dao.observeTrackedApp(id), installedApps.packageChanges) { app, _ -> app }
                .mapLatest { app -> app?.withInstalledApp() }
                .flowOn(ioDispatcher)

        override suspend fun findTrackedApp(repository: GitHubRepository): Long? =
            dao.findByRepository(owner = repository.owner, name = repository.name)?.id

        override suspend fun lookUpRepository(repository: GitHubRepository): SourceResult<RepositoryPreview> {
            val ref = repository.toRef()
            return when (val details = network.repository(ref).toSourceResult()) {
                is SourceResult.Failure -> details
                is SourceResult.Success -> previewReleases(ref).map { releases -> details.data.toPreview(releases) }
            }
        }

        /** The newest releases; a repository without releases answers 404. */
        private suspend fun previewReleases(ref: RepositoryRef): SourceResult<List<NetworkRelease>> =
            when (val result = network.releases(ref, perPage = PREVIEW_RELEASE_COUNT)) {
                is GitHubResult.Failure if result.error == GitHubError.NotFound -> SourceResult.Success(emptyList())
                else -> result.toSourceResult()
            }

        override suspend fun addApp(
            preview: RepositoryPreview,
            options: AddAppOptions,
        ): Long {
            findTrackedApp(preview.repository)?.let { return it }
            val now = clock.millis()
            val id =
                dao.insertTrackedApp(
                    TrackedAppEntity(
                        owner = preview.repository.owner,
                        name = preview.repository.name,
                        displayName = preview.repository.name,
                        description = preview.description,
                        ownerAvatarUrl = preview.ownerAvatarUrl,
                        isPrivate = preview.isPrivate,
                        packageName = options.packageName,
                        includePrereleases = options.includePrereleases,
                        preferredAssetName = options.preferredAssetName,
                        addedAt = now,
                        lastCheckedAt = now,
                        releaseEtag = null,
                    ),
                )
            val release = preview.latestRelease(options.includePrereleases)
            if (release != null) {
                dao.replaceLatestRelease(id, release.toEntity(id), release.assetEntities(id))
            }
            return id
        }

        override suspend fun trackRepository(
            repository: GitHubRepository,
            packageName: String,
            displayName: String,
            includePrereleases: Boolean,
        ): Long =
            findTrackedApp(repository) ?: dao.insertTrackedApp(
                TrackedAppEntity(
                    owner = repository.owner,
                    name = repository.name,
                    displayName = displayName,
                    description = null,
                    ownerAvatarUrl = null,
                    isPrivate = false,
                    packageName = packageName,
                    includePrereleases = includePrereleases,
                    preferredAssetName = null,
                    addedAt = clock.millis(),
                    lastCheckedAt = null,
                    releaseEtag = null,
                ),
            )

        override suspend fun removeApp(id: Long) {
            dao.deleteTrackedApp(id)
        }

        override suspend fun setPackageName(
            id: Long,
            packageName: String?,
        ) {
            dao.setPackageName(id, packageName)
        }

        override suspend fun setPreferredAssetName(
            id: Long,
            assetName: String?,
        ) {
            dao.setPreferredAssetName(id, assetName)
        }

        override suspend fun setIncludePrereleases(
            id: Long,
            includePrereleases: Boolean,
        ): UpdateCheckResult {
            dao.setIncludePrereleases(id, includePrereleases)
            val app = dao.getTrackedApps().firstOrNull { it.app.id == id }
            return releaseChecker.check(listOfNotNull(app))
        }

        override suspend fun checkForUpdates(): UpdateCheckResult = releaseChecker.check(dao.getTrackedApps())

        private suspend fun TrackedAppWithRelease.withInstalledApp(): TrackedApp =
            toTrackedApp(installedApp = app.packageName?.let { installedApps.installedApp(it) })

        private companion object {
            /** Releases shown when adding an app; enough to find the newest stable one in most repositories. */
            const val PREVIEW_RELEASE_COUNT = 20
        }
    }
