package me.geckotv.apkmanager.core.data

import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkAuthenticatedUser
import me.geckotv.apkmanager.core.network.model.NetworkOwner
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkReleaseAsset
import me.geckotv.apkmanager.core.network.model.NetworkReleaseSummary
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import okhttp3.ResponseBody

/** In-memory GitHub: repositories with releases (newest first) and a log of the calls made. */
class FakeGitHubNetworkDataSource : GitHubNetworkDataSource {
    val repositories = mutableMapOf<RepositoryRef, NetworkRepository>()
    val releases = mutableMapOf<RepositoryRef, List<NetworkRelease>>()

    /** ETag returned with every successful release response, and accepted for `304`. */
    var etag: String? = "\"etag-1\""

    /** When set, every call fails with this error. */
    var error: GitHubError? = null

    val calls = mutableListOf<String>()

    fun addRepository(
        ref: RepositoryRef,
        vararg releases: NetworkRelease,
        isPrivate: Boolean = false,
    ) {
        repositories[ref] =
            NetworkRepository(
                id = repositories.size + 1L,
                name = ref.name,
                fullName = ref.toString(),
                owner = NetworkOwner(login = ref.owner, avatarUrl = "https://avatars.example/${ref.owner}"),
                description = "Description of $ref",
                isPrivate = isPrivate,
                htmlUrl = "https://github.com/$ref",
            )
        this.releases[ref] = releases.toList()
    }

    override suspend fun authenticatedUser(token: String): GitHubResult<NetworkAuthenticatedUser> =
        throw UnsupportedOperationException()

    override suspend fun repository(repository: RepositoryRef): GitHubResult<NetworkRepository> {
        calls += "repository $repository"
        error?.let { return GitHubResult.Failure(it) }
        return repositories[repository]?.let { GitHubResult.Success(it) } ?: GitHubResult.Failure(GitHubError.NotFound)
    }

    override suspend fun latestRelease(
        repository: RepositoryRef,
        etag: String?,
    ): GitHubResult<NetworkRelease> {
        calls += "latestRelease $repository etag=$etag"
        val latest = releases[repository]?.firstOrNull { !it.draft && !it.prerelease }
        val failure = error
        return when {
            failure != null -> GitHubResult.Failure(failure)
            etag != null && etag == this.etag -> GitHubResult.NotModified
            latest == null -> GitHubResult.Failure(GitHubError.NotFound)
            else -> GitHubResult.Success(latest, this.etag)
        }
    }

    override suspend fun releases(
        repository: RepositoryRef,
        perPage: Int,
        page: Int,
        etag: String?,
    ): GitHubResult<List<NetworkRelease>> {
        calls += "releases $repository etag=$etag"
        val list = releases[repository]
        val failure = error
        return when {
            failure != null -> GitHubResult.Failure(failure)
            etag != null && etag == this.etag -> GitHubResult.NotModified
            list == null -> GitHubResult.Failure(GitHubError.NotFound)
            else -> GitHubResult.Success(list.take(perPage), this.etag)
        }
    }

    override suspend fun latestReleases(
        repositories: List<RepositoryRef>,
    ): GitHubResult<Map<RepositoryRef, NetworkReleaseSummary?>> {
        calls += "latestReleases ${repositories.joinToString()}"
        error?.let { return GitHubResult.Failure(it) }
        return GitHubResult.Success(
            repositories.associateWith { ref ->
                releases[ref]?.firstOrNull { !it.draft && !it.prerelease }?.let { release ->
                    NetworkReleaseSummary(
                        id = release.id,
                        tagName = release.tagName,
                        name = release.name,
                        isPrerelease = false,
                        publishedAt = release.publishedAt,
                        htmlUrl = release.htmlUrl,
                    )
                }
            },
        )
    }

    override suspend fun downloadAsset(
        repository: RepositoryRef,
        assetId: Long,
    ): GitHubResult<ResponseBody> = throw UnsupportedOperationException()
}

fun networkRelease(
    id: Long,
    tag: String,
    prerelease: Boolean = false,
    draft: Boolean = false,
    assetNames: List<String> = listOf("app-$tag.apk"),
) = NetworkRelease(
    id = id,
    tagName = tag,
    name = "Release $tag",
    body = "Notes for $tag",
    draft = draft,
    prerelease = prerelease,
    publishedAt = "2026-11-0${id % 9 + 1}T12:00:00Z",
    htmlUrl = "https://github.com/octo/app/releases/tag/$tag",
    assets =
        assetNames.mapIndexed { index, name ->
            NetworkReleaseAsset(
                id = id * 100 + index,
                name = name,
                contentType = "application/vnd.android.package-archive",
                size = 1_000,
                digest = "sha256:ABC$index",
                browserDownloadUrl = "https://github.com/octo/app/releases/download/$tag/$name",
                url = "https://api.github.com/repos/octo/app/releases/assets/${id * 100 + index}",
            )
        },
)
