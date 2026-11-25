package me.geckotv.apkmanager.core.auth

import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import java.time.Instant

/** Signing in to GitHub and the current sign-in state. */
interface AuthRepository {
    /** The current sign-in state; emits again whenever it changes. */
    val authState: Flow<AuthState>

    /**
     * Starts the OAuth device flow. Show [DeviceCode.userCode] to the user and send them to
     * [DeviceCode.verificationUri], then call [completeDeviceFlow].
     */
    suspend fun startDeviceFlow(accessLevel: GitHubAccessLevel): DeviceFlowStart

    /** Waits until the user authorized (or denied) the device code, then stores the token. */
    suspend fun completeDeviceFlow(deviceCode: DeviceCode): SignInResult

    /** Signs in with a personal access token after checking it against the API. */
    suspend fun signInWithToken(token: String): SignInResult

    /**
     * Reloads the account details from GitHub. Marks the token invalid when GitHub rejects it.
     * Does nothing when signed out.
     */
    suspend fun refreshAccount()

    /** Forgets the token and the account. */
    suspend fun signOut()
}

/** Whether the user is signed in. */
sealed interface AuthState {
    data object SignedOut : AuthState

    /**
     * @property tokenInvalid GitHub rejected the stored token (expired or revoked); the user should
     *   sign in again.
     */
    data class SignedIn(
        val account: GitHubAccount,
        val tokenInvalid: Boolean = false,
    ) : AuthState
}

/** A device code the user enters on GitHub to authorize the app. */
data class DeviceCode(
    val userCode: String,
    val verificationUri: String,
    val expiresAt: Instant,
    internal val deviceCode: String,
    internal val intervalSeconds: Int,
    internal val accessLevel: GitHubAccessLevel,
)

/** Result of [AuthRepository.startDeviceFlow]. */
sealed interface DeviceFlowStart {
    data class Started(
        val deviceCode: DeviceCode,
    ) : DeviceFlowStart

    data class Failed(
        val reason: SignInFailure,
    ) : DeviceFlowStart
}

/** Result of a sign-in attempt. */
sealed interface SignInResult {
    data class Success(
        val account: GitHubAccount,
    ) : SignInResult

    data class Failed(
        val reason: SignInFailure,
    ) : SignInResult
}

/** Why signing in failed. */
enum class SignInFailure {
    /** The device code expired before the user authorized it. */
    Expired,

    /** The user denied the authorization on GitHub. */
    Denied,

    /** GitHub rejected the personal access token. */
    InvalidToken,

    /** No connection to GitHub. */
    Network,

    /** Any other problem, e.g. device flow is not enabled for the OAuth app. */
    Unknown,
}
