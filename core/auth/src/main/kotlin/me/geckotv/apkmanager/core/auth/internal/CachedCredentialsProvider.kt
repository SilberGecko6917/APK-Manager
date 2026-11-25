package me.geckotv.apkmanager.core.auth.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the stored token to the network layer from an in-memory cache that follows the
 * [CredentialsStore]. It deliberately does not depend on the network itself, so the OkHttp client
 * and the auth repository do not depend on each other.
 */
@Singleton
internal class CachedCredentialsProvider
    @Inject
    constructor(
        private val credentialsStore: CredentialsStore,
        @ApplicationScope applicationScope: CoroutineScope,
    ) : GitHubCredentialsProvider {
        @Volatile
        private var cachedToken: String? = null

        @Volatile
        private var cacheLoaded = false

        init {
            applicationScope.launch {
                credentialsStore.credentials.collect { credentials ->
                    cachedToken = credentials?.token
                    cacheLoaded = true
                }
            }
        }

        /** Called by OkHttp for every request; reads the store once if the cache is not ready yet. */
        override fun token(): String? {
            if (!cacheLoaded) {
                cachedToken = runBlocking { credentialsStore.credentials.first()?.token }
                cacheLoaded = true
            }
            return cachedToken
        }
    }
