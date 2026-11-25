package me.geckotv.apkmanager.core.auth.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.geckotv.apkmanager.core.auth.AuthRepository
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.auth.DeviceCode
import me.geckotv.apkmanager.core.auth.DeviceFlowStart
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.auth.SignInResult
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.GitHubResult
import me.geckotv.apkmanager.core.network.model.NetworkAuthenticatedUser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signs in with the device flow or a personal access token, stores the token encrypted and
 * supplies it to the network layer.
 */
@Singleton
internal class DefaultAuthRepository
    @Inject
    constructor(
        private val deviceFlowClient: DeviceFlowClient,
        private val credentialsStore: CredentialsStore,
        private val network: GitHubNetworkDataSource,
        @ApplicationScope applicationScope: CoroutineScope,
    ) : AuthRepository,
        GitHubCredentialsProvider {
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

        override val authState: Flow<AuthState> =
            credentialsStore.credentials.map { credentials ->
                if (credentials == null) {
                    AuthState.SignedOut
                } else {
                    AuthState.SignedIn(credentials.account, credentials.tokenInvalid)
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

        override suspend fun startDeviceFlow(accessLevel: GitHubAccessLevel): DeviceFlowStart =
            when (val result = deviceFlowClient.start(accessLevel)) {
                is DeviceFlowClient.StartResult.Started -> DeviceFlowStart.Started(result.deviceCode)
                is DeviceFlowClient.StartResult.Failed -> DeviceFlowStart.Failed(result.reason)
            }

        override suspend fun completeDeviceFlow(deviceCode: DeviceCode): SignInResult =
            when (val result = deviceFlowClient.awaitToken(deviceCode)) {
                is DeviceFlowClient.TokenResult.Failed -> {
                    SignInResult.Failed(result.reason)
                }

                is DeviceFlowClient.TokenResult.Success -> {
                    signIn(
                        token = result.accessToken,
                        authMethod = AuthMethod.OAuth,
                        accessLevelFromScopes = { result.scopes.toAccessLevel() },
                    )
                }
            }

        override suspend fun signInWithToken(token: String): SignInResult =
            signIn(
                token = token.trim(),
                authMethod = AuthMethod.PersonalAccessToken,
                accessLevelFromScopes = { user -> user.scopes?.toAccessLevel() },
            )

        override suspend fun refreshAccount() {
            val credentials = credentialsStore.credentials.first() ?: return
            when (val result = network.authenticatedUser(credentials.token)) {
                is GitHubResult.Success -> {
                    val user = result.data.user
                    credentialsStore.updateAccount(user.login, user.name, user.avatarUrl)
                }

                is GitHubResult.Failure -> {
                    if (result.error ==
                        GitHubError.Unauthorized
                    ) {
                        credentialsStore.markTokenInvalid()
                    }
                }

                GitHubResult.NotModified -> {
                    Unit
                }
            }
        }

        override suspend fun signOut() {
            credentialsStore.clear()
        }

        private suspend fun signIn(
            token: String,
            authMethod: AuthMethod,
            accessLevelFromScopes: (NetworkAuthenticatedUser) -> GitHubAccessLevel?,
        ): SignInResult {
            if (token.isBlank()) return SignInResult.Failed(SignInFailure.InvalidToken)
            return when (val result = network.authenticatedUser(token)) {
                is GitHubResult.Success -> {
                    val user = result.data.user
                    val account =
                        GitHubAccount(
                            login = user.login,
                            name = user.name,
                            avatarUrl = user.avatarUrl,
                            authMethod = authMethod,
                            accessLevel = accessLevelFromScopes(result.data),
                        )
                    credentialsStore.save(token, account)
                    SignInResult.Success(account)
                }

                is GitHubResult.Failure -> {
                    SignInResult.Failed(result.error.toSignInFailure())
                }

                GitHubResult.NotModified -> {
                    SignInResult.Failed(SignInFailure.Unknown)
                }
            }
        }

        private fun GitHubError.toSignInFailure(): SignInFailure =
            when (this) {
                GitHubError.Unauthorized -> SignInFailure.InvalidToken
                is GitHubError.Network -> SignInFailure.Network
                else -> SignInFailure.Unknown
            }

        private fun Set<String>.toAccessLevel(): GitHubAccessLevel =
            if ("repo" in this) GitHubAccessLevel.Private else GitHubAccessLevel.Public
    }
