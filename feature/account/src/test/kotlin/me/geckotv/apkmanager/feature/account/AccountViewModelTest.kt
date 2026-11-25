package me.geckotv.apkmanager.feature.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.auth.DeviceCode
import me.geckotv.apkmanager.core.auth.DeviceFlowStart
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.auth.SignInResult
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.network.RateLimitTracker
import me.geckotv.apkmanager.core.testing.FakeAuthRepository
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val rateLimitTracker = RateLimitTracker()
    private lateinit var viewModel: AccountViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = AccountViewModel(authRepository, rateLimitTracker)
    }

    @Test
    fun startsSignedOutWithPrivateAccess() =
        runTest {
            collectUiState()
            val state = ready()

            assertEquals(AuthState.SignedOut, state.authState)
            assertEquals(GitHubAccessLevel.Private, state.accessLevel)
            assertNull(state.sheet)
            assertNull(state.error)
        }

    @Test
    fun refreshesTheAccountOnInit() {
        assertEquals(1, authRepository.refreshCount)
    }

    @Test
    fun selectedAccessLevelIsPassedToTheDeviceFlow() =
        runTest {
            collectUiState()
            authRepository.deviceFlowStart = DeviceFlowStart.Started(deviceCode())

            viewModel.selectAccessLevel(GitHubAccessLevel.Public)
            viewModel.startDeviceFlow()

            assertEquals(GitHubAccessLevel.Public, authRepository.lastAccessLevel)
        }

    @Test
    fun successfulDeviceFlowSignsInAndClosesTheSheet() =
        runTest {
            collectUiState()
            authRepository.deviceFlowStart = DeviceFlowStart.Started(deviceCode())

            viewModel.startDeviceFlow()

            val state = ready()
            assertTrue(state.authState is AuthState.SignedIn)
            assertNull(state.sheet)
            assertNull(state.error)
            assertEquals(false, state.isWorking)
        }

    @Test
    fun deniedDeviceFlowShowsTheError() =
        runTest {
            collectUiState()
            authRepository.deviceFlowStart = DeviceFlowStart.Started(deviceCode())
            authRepository.deviceFlowResult = SignInResult.Failed(SignInFailure.Denied)

            viewModel.startDeviceFlow()

            val state = ready()
            assertEquals(AuthState.SignedOut, state.authState)
            assertEquals(SignInFailure.Denied, state.error)
            assertNull(state.sheet)

            viewModel.clearError()
            assertNull(ready().error)
        }

    @Test
    fun failedDeviceFlowStartShowsTheError() =
        runTest {
            collectUiState()
            authRepository.deviceFlowStart = DeviceFlowStart.Failed(SignInFailure.Network)

            viewModel.startDeviceFlow()

            assertEquals(SignInFailure.Network, ready().error)
        }

    @Test
    fun tokenSignInPassesTheTrimmedToken() =
        runTest {
            collectUiState()
            viewModel.showTokenSheet()
            assertEquals(AccountSheet.Token, ready().sheet)

            viewModel.signInWithToken("  ghp_secret \n")

            assertEquals("ghp_secret", authRepository.lastToken)
            val state = ready()
            val account = (state.authState as AuthState.SignedIn).account
            assertEquals(AuthMethod.PersonalAccessToken, account.authMethod)
            assertNull(state.sheet)
        }

    @Test
    fun blankTokenIsIgnored() =
        runTest {
            collectUiState()

            viewModel.signInWithToken("   ")

            assertNull(authRepository.lastToken)
        }

    @Test
    fun invalidTokenKeepsTheSheetOpenWithAnError() =
        runTest {
            collectUiState()
            authRepository.tokenSignInResult = SignInResult.Failed(SignInFailure.InvalidToken)
            viewModel.showTokenSheet()

            viewModel.signInWithToken("bad")

            val state = ready()
            assertEquals(AccountSheet.Token, state.sheet)
            assertEquals(SignInFailure.InvalidToken, state.error)
        }

    @Test
    fun signOutReturnsToSignedOut() =
        runTest {
            collectUiState()
            viewModel.signInWithToken("token")
            assertTrue(ready().authState is AuthState.SignedIn)

            viewModel.signOut()

            assertEquals(AuthState.SignedOut, ready().authState)
        }

    private fun TestScope.collectUiState() {
        // uiState only collects its upstream while it has a subscriber.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun ready(): AccountUiState.Ready {
        val uiState: StateFlow<AccountUiState> = viewModel.uiState
        return uiState.value as AccountUiState.Ready
    }

    private fun deviceCode() =
        DeviceCode(
            userCode = "ABCD-1234",
            verificationUri = "https://github.com/login/device",
            expiresAt = Instant.now().plusSeconds(900),
            deviceCode = "dc",
            intervalSeconds = 5,
            accessLevel = GitHubAccessLevel.Private,
        )
}
