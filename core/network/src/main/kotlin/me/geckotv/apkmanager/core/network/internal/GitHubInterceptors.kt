package me.geckotv.apkmanager.core.network.internal

import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import me.geckotv.apkmanager.core.network.RateLimitTracker
import me.geckotv.apkmanager.core.network.model.RateLimit
import okhttp3.Interceptor
import okhttp3.Response
import java.time.Instant

internal const val GITHUB_API_VERSION = "2022-11-28"
internal const val USER_AGENT = "APK-Manager (+https://github.com/SilberGecko6917/APK-Manager)"
internal const val ACCEPT_JSON = "application/vnd.github+json"

/**
 * Adds the headers GitHub expects and, for requests to the API host only, the user's token.
 *
 * Redirects to other hosts (e.g. release asset storage) never carry the token: application
 * interceptors only see the original request, and OkHttp drops `Authorization` when it follows a
 * redirect to another host.
 */
internal class GitHubRequestInterceptor(
    private val apiHost: String,
    private val credentialsProvider: GitHubCredentialsProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != apiHost) return chain.proceed(request)

        val builder =
            request
                .newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
        if (request.header("Accept") == null) builder.header("Accept", ACCEPT_JSON)
        // A request may carry its own token, e.g. to validate a token before storing it.
        if (request.header("Authorization") == null) {
            credentialsProvider.token()?.takeIf { it.isNotBlank() }?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        }
        return chain.proceed(builder.build())
    }
}

/** Records the `x-ratelimit-*` headers of every API response in the [RateLimitTracker]. */
internal class RateLimitInterceptor(
    private val tracker: RateLimitTracker,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        response.rateLimit()?.let(tracker::update)
        return response
    }
}

internal fun Response.rateLimit(): RateLimit? {
    val limit = header("x-ratelimit-limit")?.toIntOrNull()
    val remaining = header("x-ratelimit-remaining")?.toIntOrNull()
    val reset = header("x-ratelimit-reset")?.toLongOrNull()
    if (limit == null || remaining == null || reset == null) return null
    return RateLimit(
        resource = header("x-ratelimit-resource") ?: "core",
        limit = limit,
        remaining = remaining,
        resetAt = Instant.ofEpochSecond(reset),
    )
}
