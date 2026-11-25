package me.geckotv.apkmanager.core.network

import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import okhttp3.ResponseBody

/** Access to the GitHub API. */
interface GitHubNetworkDataSource {
    suspend fun repository(repository: RepositoryRef): GitHubResult<NetworkRepository>

    /**
     * The latest non-draft, non-prerelease release. Pass the [etag] of the previous response to get
     * [GitHubResult.NotModified] (which does not count against the rate limit when signed in).
     */
    suspend fun latestRelease(
        repository: RepositoryRef,
        etag: String? = null,
    ): GitHubResult<NetworkRelease>

    /** Releases including pre-releases, newest first. */
    suspend fun releases(
        repository: RepositoryRef,
        perPage: Int = DEFAULT_PAGE_SIZE,
        page: Int = 1,
        etag: String? = null,
    ): GitHubResult<List<NetworkRelease>>

    /**
     * The latest release of many repositories, fetched with batched GraphQL queries. Requires a
     * token; without one the result is [me.geckotv.apkmanager.core.network.model.GitHubError.Unauthorized].
     * Repositories that do not exist or have no release map to `null`.
     */
    suspend fun latestReleases(
        repositories: List<RepositoryRef>,
    ): GitHubResult<Map<RepositoryRef, NetworkReleaseSummary?>>

    /**
     * Opens a release asset for download. Works for private repositories when a token with access is
     * set. The caller must close the returned body.
     */
    suspend fun downloadAsset(
        repository: RepositoryRef,
        assetId: Long,
    ): GitHubResult<ResponseBody>

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
    }
}
