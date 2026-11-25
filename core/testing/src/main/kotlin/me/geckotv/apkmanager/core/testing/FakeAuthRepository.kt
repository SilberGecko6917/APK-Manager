package me.geckotv.apkmanager.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.geckotv.apkmanager.core.auth.AuthRepository
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.auth.DeviceCode
import me.geckotv.apkmanager.core.auth.DeviceFlowStart
import me.geckotv.apkmanager.core.auth.SignInResult
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount

/**
 * In-memory [AuthRepository] for tests. Set [deviceFlowStart], [deviceFlowResult] and
 * [tokenSignInResult] to control the outcomes.
 */
class FakeAuthRepository(
    initial: AuthState = AuthState.SignedOut,
) : AuthRepository {
    private val state = MutableStateFlow(initial)

    override val authState: Flow<AuthState> = state

    var deviceFlowStart: DeviceFlowStart? = null
    var deviceFlowResult: SignInResult = SignInResult.Success(sampleAccount())
    var tokenSignInResult: SignInResult = SignInResult.Success(sampleAccount(AuthMethod.PersonalAccessToken))
    var refreshCount = 0
        private set
    var lastAccessLevel: GitHubAccessLevel? = null
        private set
    var lastToken: String? = null
        private set

    override suspend fun startDeviceFlow(accessLevel: GitHubAccessLevel): DeviceFlowStart {
        lastAccessLevel = accessLevel
        return checkNotNull(deviceFlowStart) { "Set deviceFlowStart before starting the device flow" }
    }

    override suspend fun completeDeviceFlow(deviceCode: DeviceCode): SignInResult =
        deviceFlowResult.also { result ->
            if (result is SignInResult.Success) {
                state.value =
                    AuthState.SignedIn(result.account)
            }
        }

    override suspend fun signInWithToken(token: String): SignInResult {
        lastToken = token
        return tokenSignInResult.also { result ->
            if (result is SignInResult.Success) state.value = AuthState.SignedIn(result.account)
        }
    }

    override suspend fun refreshAccount() {
        refreshCount++
    }

    override suspend fun signOut() {
        state.value = AuthState.SignedOut
    }

    companion object {
        fun sampleAccount(authMethod: AuthMethod = AuthMethod.OAuth): GitHubAccount =
            GitHubAccount(
                login = "octocat",
                name = "The Octocat",
                avatarUrl = null,
                authMethod = authMethod,
                accessLevel = GitHubAccessLevel.Private,
            )
    }
}
