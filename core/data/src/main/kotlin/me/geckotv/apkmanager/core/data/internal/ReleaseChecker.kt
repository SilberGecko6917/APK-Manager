package me.geckotv.apkmanager.core.data.internal

import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.database.dao.TrackedAppDao
import me.geckotv.apkmanager.core.database.model.TrackedAppWithRelease
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import java.time.Clock
import javax.inject.Inject

/**
 * Fetches the latest release of tracked apps and stores it, using as few requests as possible:
 *
 * - **Signed in:** one GraphQL query per 50 apps returns the latest release IDs. Only apps whose
 *   release changed are fetched again through REST (for the assets and notes).
 * - **Signed out:** `GET …/releases/latest` per app with the stored ETag.
 * - **Pre-releases included:** `GET …/releases` per app (with ETag), because "latest" skips them.
 *
 * Checking stops once the rate limit is hit; the remaining apps are reported as rate limited.
 */
internal class ReleaseChecker
    @Inject
    constructor(
        private val network: GitHubNetworkDataSource,
        private val credentialsProvider: GitHubCredentialsProvider,
        private val dao: TrackedAppDao,
        private val clock: Clock,
    ) {
        suspend fun check(apps: List<TrackedAppWithRelease>): UpdateCheckResult {
            val summaries = latestReleaseSummaries(apps.filterNot { it.app.includePrereleases })
            val failures = mutableMapOf<Long, SourceError>()
            var rateLimit: SourceError.RateLimited? = null
            for (app in apps) {
                val error =
                    rateLimit ?: when {
                        app.app.includePrereleases -> checkReleaseList(app)
                        summaries != null -> checkSummary(app, summaries[app.repository.toRef()])
                        else -> checkLatestRelease(app)
                    }
                if (error != null) failures[app.app.id] = error
                if (error is SourceError.RateLimited) rateLimit = error
            }
            return UpdateCheckResult(checkedCount = apps.size - failures.size, failures = failures)
        }

        /** Latest releases from the batched GraphQL query, or `null` when signed out or it failed. */
        private suspend fun latestReleaseSummaries(
            apps: List<TrackedAppWithRelease>,
        ): Map<RepositoryRef, NetworkReleaseSummary?>? {
            if (apps.isEmpty() || credentialsProvider.token().isNullOrBlank()) return null
            val result = network.latestReleases(apps.map { it.repository.toRef() })
            return (result as? GitHubResult.Success)?.data
        }

        private suspend fun checkSummary(
            app: TrackedAppWithRelease,
            summary: NetworkReleaseSummary?,
        ): SourceError? =
            when {
                summary == null -> {
                    storeRelease(app, release = null, etag = null)
                    null
                }

                summary.id == app.latestRelease?.releaseId -> {
                    dao.setChecked(app.app.id, checkedAt = now(), etag = app.app.releaseEtag)
                    null
                }

                else -> {
                    checkLatestRelease(app, etag = null)
                }
            }

        private suspend fun checkLatestRelease(
            app: TrackedAppWithRelease,
            etag: String? = app.app.releaseEtag.takeIf { app.latestRelease != null },
        ): SourceError? =
            handle(app, network.latestRelease(app.repository.toRef(), etag)) { release ->
                release.takeUnless { it.draft || it.prerelease }?.toRelease()
            }

        private suspend fun checkReleaseList(app: TrackedAppWithRelease): SourceError? {
            val etag = app.app.releaseEtag.takeIf { app.latestRelease != null }
            val result = network.releases(app.repository.toRef(), perPage = RELEASE_LIST_SIZE, etag = etag)
            return handle(app, result) { releases ->
                releases.firstOrNull { !it.draft }?.toRelease()
            }
        }

        private suspend fun <T> handle(
            app: TrackedAppWithRelease,
            result: GitHubResult<T>,
            latestRelease: (T) -> Release?,
        ): SourceError? =
            when (result) {
                is GitHubResult.Success -> {
                    storeRelease(app, latestRelease(result.data), result.etag)
                    null
                }

                GitHubResult.NotModified -> {
                    dao.setChecked(app.app.id, checkedAt = now(), etag = app.app.releaseEtag)
                    null
                }

                // No (published) release. A repository that disappeared looks the same and is shown
                // as "no release" rather than as an error.
                is GitHubResult.Failure if result.error == GitHubError.NotFound -> {
                    storeRelease(app, release = null, etag = null)
                    null
                }

                is GitHubResult.Failure -> {
                    result.error.toSourceError()
                }
            }

        private suspend fun storeRelease(
            app: TrackedAppWithRelease,
            release: Release?,
            etag: String?,
        ) {
            val appId = app.app.id
            dao.replaceLatestRelease(appId, release?.toEntity(appId), release?.assetEntities(appId).orEmpty())
            dao.setChecked(appId, checkedAt = now(), etag = etag)
        }

        private fun now(): Long = clock.millis()

        private companion object {
            /** Enough to skip a few drafts; only the newest published release is used. */
            const val RELEASE_LIST_SIZE = 10
        }
    }
