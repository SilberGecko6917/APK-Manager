package me.geckotv.apkmanager.core.data.internal

import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkRelease
import me.geckotv.apkmanager.core.network.model.NetworkReleaseAsset
import me.geckotv.apkmanager.core.network.model.NetworkRepository
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import java.time.Instant
import java.time.format.DateTimeParseException

private const val SHA256_PREFIX = "sha256:"

internal fun GitHubRepository.toRef(): RepositoryRef = RepositoryRef(owner = owner, name = name)

internal fun GitHubError.toSourceError(): SourceError =
    when (this) {
        GitHubError.NotFound -> SourceError.NotFound
        GitHubError.Unauthorized -> SourceError.Unauthorized
        is GitHubError.Forbidden -> SourceError.Forbidden(message)
        is GitHubError.RateLimited -> SourceError.RateLimited(retryAt)
        is GitHubError.Server -> SourceError.Unavailable
        is GitHubError.Network -> SourceError.Offline
        is GitHubError.Unexpected -> SourceError.Unexpected(message)
    }

internal fun <T> GitHubResult<T>.toSourceResult(): SourceResult<T> =
    when (this) {
        is GitHubResult.Success -> SourceResult.Success(data)
        is GitHubResult.Failure -> SourceResult.Failure(error.toSourceError())
        GitHubResult.NotModified -> SourceResult.Failure(SourceError.Unexpected("Unexpected 304 response"))
    }

internal inline fun <T, R> SourceResult<T>.map(transform: (T) -> R): SourceResult<R> =
    when (this) {
        is SourceResult.Success -> SourceResult.Success(transform(data))
        is SourceResult.Failure -> this
    }

internal fun NetworkRepository.toPreview(releases: List<NetworkRelease>): RepositoryPreview =
    RepositoryPreview(
        repository = GitHubRepository(owner = owner.login, name = name),
        description = description,
        ownerAvatarUrl = owner.avatarUrl,
        isPrivate = isPrivate,
        isArchived = archived,
        stars = stars,
        releases = releases.filterNot { it.draft }.map { it.toRelease() },
    )

internal fun NetworkRelease.toRelease(): Release =
    Release(
        id = id,
        tagName = tagName,
        name = name?.takeIf(String::isNotBlank),
        body = body?.takeIf(String::isNotBlank),
        isPrerelease = prerelease,
        publishedAt = (publishedAt ?: createdAt)?.toInstantOrNull(),
        htmlUrl = htmlUrl,
        assets = assets.map { it.toAsset() },
    )

private fun NetworkReleaseAsset.toAsset(): ReleaseAsset =
    ReleaseAsset(
        id = id,
        name = name,
        contentType = contentType,
        size = size,
        sha256 =
            digest
                ?.takeIf { it.startsWith(SHA256_PREFIX, ignoreCase = true) }
                ?.substring(SHA256_PREFIX.length)
                ?.lowercase(),
        downloadCount = downloadCount,
        browserDownloadUrl = browserDownloadUrl,
    )

private fun String.toInstantOrNull(): Instant? =
    try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
