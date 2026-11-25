package me.geckotv.apkmanager.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.geckotv.apkmanager.core.network.model.RateLimit
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the latest known rate limit per resource (`core`, `graphql`, `search`, …). */
@Singleton
class RateLimitTracker
    @Inject
    constructor() {
        private val state = MutableStateFlow<Map<String, RateLimit>>(emptyMap())

        /** The latest rate limit per resource name. */
        val rateLimits: StateFlow<Map<String, RateLimit>> = state.asStateFlow()

        internal fun update(rateLimit: RateLimit) {
            state.update { it + (rateLimit.resource to rateLimit) }
        }
    }
