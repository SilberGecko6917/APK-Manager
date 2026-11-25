package me.geckotv.apkmanager.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.auth.AuthRepository
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.auth.DeviceFlowStart
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.auth.SignInResult
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.network.RateLimitTracker
import me.geckotv.apkmanager.core.network.model.RateLimit
import javax.inject.Inject

@HiltViewModel
class AccountViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        rateLimitTracker: RateLimitTracker,
    ) : ViewModel() {
        private val local = MutableStateFlow(LocalState())
        private var signInJob: Job? = null

        val uiState: StateFlow<AccountUiState> =
            combine(
                authRepository.authState,
                rateLimitTracker.rateLimits,
                local,
            ) { authState, rateLimits, localState ->
                AccountUiState.Ready(
                    authState = authState,
                    rateLimit = rateLimits[CORE_RESOURCE],
                    accessLevel = localState.accessLevel,
                    sheet = localState.sheet,
                    isWorking = localState.isWorking,
                    error = localState.error,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AccountUiState.Loading,
            )

        init {
            viewModelScope.launch { authRepository.refreshAccount() }
        }

        fun selectAccessLevel(accessLevel: GitHubAccessLevel) {
            local.update { it.copy(accessLevel = accessLevel) }
        }

        /** Starts the OAuth device flow and shows the code sheet once GitHub issued a code. */
        fun startDeviceFlow() {
            signInJob?.cancel()
            local.update { it.copy(isWorking = true, error = null) }
            signInJob =
                viewModelScope.launch {
                    when (val start = authRepository.startDeviceFlow(local.value.accessLevel)) {
                        is DeviceFlowStart.Failed -> {
                            finishSignIn(start.reason)
                        }

                        is DeviceFlowStart.Started -> {
                            val code = start.deviceCode
                            local.update {
                                it.copy(sheet = AccountSheet.DeviceCode(code.userCode, code.verificationUri))
                            }
                            val result = authRepository.completeDeviceFlow(code)
                            finishSignIn((result as? SignInResult.Failed)?.reason)
                        }
                    }
                }
        }

        fun showTokenSheet() {
            local.update { it.copy(sheet = AccountSheet.Token, error = null) }
        }

        /** Signs in with a personal access token; the sheet stays open when GitHub rejects it. */
        fun signInWithToken(token: String) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return
            signInJob?.cancel()
            local.update { it.copy(isWorking = true, error = null) }
            signInJob =
                viewModelScope.launch {
                    val failure = (authRepository.signInWithToken(trimmed) as? SignInResult.Failed)?.reason
                    local.update {
                        it.copy(
                            isWorking = false,
                            error = failure,
                            sheet = if (failure == null) null else it.sheet,
                        )
                    }
                }
        }

        /** Closes the open sheet and abandons a sign-in that is still running. */
        fun dismissSheet() {
            cancelSignIn()
        }

        /** Abandons the running sign-in, e.g. while waiting for the user to authorize the device code. */
        fun cancelSignIn() {
            signInJob?.cancel()
            signInJob = null
            local.update { it.copy(sheet = null, isWorking = false) }
        }

        fun signOut() {
            viewModelScope.launch {
                authRepository.signOut()
                local.update { it.copy(error = null) }
            }
        }

        fun clearError() {
            local.update { it.copy(error = null) }
        }

        private fun finishSignIn(failure: SignInFailure?) {
            local.update { it.copy(sheet = null, isWorking = false, error = failure) }
        }

        private data class LocalState(
            val accessLevel: GitHubAccessLevel = GitHubAccessLevel.Private,
            val sheet: AccountSheet? = null,
            val isWorking: Boolean = false,
            val error: SignInFailure? = null,
        )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val CORE_RESOURCE = "core"
        }
    }

sealed interface AccountUiState {
    data object Loading : AccountUiState

    /**
     * @property rateLimit the latest `core` rate limit, `null` until a request was made.
     * @property accessLevel the access level chosen for the next sign-in.
     * @property sheet the bottom sheet that is currently open.
     * @property isWorking a sign-in request is running.
     * @property error why the last sign-in attempt failed.
     */
    data class Ready(
        val authState: AuthState,
        val rateLimit: RateLimit?,
        val accessLevel: GitHubAccessLevel,
        val sheet: AccountSheet?,
        val isWorking: Boolean,
        val error: SignInFailure?,
    ) : AccountUiState
}

sealed interface AccountSheet {
    data class DeviceCode(
        val userCode: String,
        val verificationUri: String,
    ) : AccountSheet

    data object Token : AccountSheet
}
