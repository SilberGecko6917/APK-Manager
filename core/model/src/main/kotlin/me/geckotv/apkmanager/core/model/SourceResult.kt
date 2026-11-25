package me.geckotv.apkmanager.core.model

/** Result of a request to a release source. */
sealed interface SourceResult<out T> {
    data class Success<T>(
        val data: T,
    ) : SourceResult<T>

    data class Failure(
        val error: SourceError,
    ) : SourceResult<Nothing>
}
