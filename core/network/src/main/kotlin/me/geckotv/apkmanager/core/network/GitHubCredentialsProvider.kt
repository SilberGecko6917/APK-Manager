package me.geckotv.apkmanager.core.network

/**
 * Supplies the GitHub token sent with API requests.
 *
 * Called on OkHttp threads for every request, so implementations must return quickly, e.g. from an
 * in-memory cache.
 */
fun interface GitHubCredentialsProvider {
    /** The token to send, or `null` to make unauthenticated requests. */
    fun token(): String?
}
