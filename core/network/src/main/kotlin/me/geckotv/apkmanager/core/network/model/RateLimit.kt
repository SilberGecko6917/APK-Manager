package me.geckotv.apkmanager.core.network.model

import java.time.Instant

/** The primary rate limit of a GitHub API resource, read from the `x-ratelimit-*` response headers. */
data class RateLimit(
    val resource: String,
    val limit: Int,
    val remaining: Int,
    val resetAt: Instant,
)
