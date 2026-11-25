package me.geckotv.apkmanager.core.model

import java.time.Instant

/** Why a request to a release source (GitHub) failed. */
sealed interface SourceError {
    /** The repository does not exist, or it is private and the account cannot see it. */
    data object NotFound : SourceError

    /** The saved token is expired or was revoked. */
    data object Unauthorized : SourceError

    /** Access was denied, e.g. an organization restricts OAuth app access. */
    data class Forbidden(
        val message: String?,
    ) : SourceError

    /** The API rate limit was reached; requests work again after [retryAt] if it is known. */
    data class RateLimited(
        val retryAt: Instant?,
    ) : SourceError

    /** The service had a problem (HTTP 5xx). */
    data object Unavailable : SourceError

    /** No connection. */
    data object Offline : SourceError

    /** Anything else, e.g. a response that could not be read. */
    data class Unexpected(
        val message: String?,
    ) : SourceError
}
