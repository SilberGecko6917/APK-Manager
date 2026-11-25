package me.geckotv.apkmanager.core.network.model

import java.time.Instant

/** Result of a GitHub API call. */
sealed interface GitHubResult<out T> {
    /** The call succeeded. [etag] can be sent as `If-None-Match` next time. */
    data class Success<T>(
        val data: T,
        val etag: String? = null,
    ) : GitHubResult<T>

    /** The resource did not change since the ETag that was sent (HTTP 304). */
    data object NotModified : GitHubResult<Nothing>

    data class Failure(
        val error: GitHubError,
    ) : GitHubResult<Nothing>
}

/** Why a GitHub API call failed. */
sealed interface GitHubError {
    /** The repository or release does not exist, or the token cannot see it (GitHub answers 404 for both). */
    data object NotFound : GitHubError

    /** The token is missing, expired or revoked (HTTP 401). */
    data object Unauthorized : GitHubError

    /** Access was denied for a reason other than rate limiting, e.g. an organization blocks the app. */
    data class Forbidden(
        val message: String?,
    ) : GitHubError

    /** The primary or a secondary rate limit was hit. Retry after [retryAt] if it is known. */
    data class RateLimited(
        val retryAt: Instant?,
    ) : GitHubError

    /** GitHub had a problem (HTTP 5xx). */
    data class Server(
        val code: Int,
    ) : GitHubError

    /** The request did not reach GitHub or the connection broke. */
    data class Network(
        val cause: Throwable,
    ) : GitHubError

    /** Any other unexpected response, e.g. a body that could not be parsed. */
    data class Unexpected(
        val code: Int?,
        val message: String?,
    ) : GitHubError
}
