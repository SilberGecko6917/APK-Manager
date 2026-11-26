package me.geckotv.apkmanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.auth.AuthRepository
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userSettingsRepository: UserSettingsRepository,
        authRepository: AuthRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(userSettingsRepository.settings, authRepository.authState) { settings, authState ->
                SettingsUiState.Success(
                    settings = settings,
                    account = (authState as? AuthState.SignedIn)?.account,
                    accountNeedsSignIn = (authState as? AuthState.SignedIn)?.tokenInvalid == true,
                ) as SettingsUiState
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SettingsUiState.Loading,
            )

        fun setThemeMode(themeMode: ThemeMode) {
            viewModelScope.launch { userSettingsRepository.setThemeMode(themeMode) }
        }

        fun setUseDynamicColor(useDynamicColor: Boolean) {
            viewModelScope.launch { userSettingsRepository.setUseDynamicColor(useDynamicColor) }
        }

        fun setUseAmoledBlack(useAmoledBlack: Boolean) {
            viewModelScope.launch { userSettingsRepository.setUseAmoledBlack(useAmoledBlack) }
        }

        fun setHapticsEnabled(hapticsEnabled: Boolean) {
            viewModelScope.launch { userSettingsRepository.setHapticsEnabled(hapticsEnabled) }
        }

        fun setBackgroundChecksEnabled(backgroundChecksEnabled: Boolean) {
            viewModelScope.launch { userSettingsRepository.setBackgroundChecksEnabled(backgroundChecksEnabled) }
        }

        fun setCheckInterval(checkInterval: UpdateCheckInterval) {
            viewModelScope.launch { userSettingsRepository.setCheckInterval(checkInterval) }
        }

        fun setCheckOnlyOnUnmeteredNetwork(checkOnlyOnUnmeteredNetwork: Boolean) {
            viewModelScope.launch { userSettingsRepository.setCheckOnlyOnUnmeteredNetwork(checkOnlyOnUnmeteredNetwork) }
        }

        /** Installs updates automatically for apps without an own choice, or only notifies about them. */
        fun setInstallUpdatesAutomatically(installAutomatically: Boolean) {
            val policy = if (installAutomatically) UpdatePolicy.Install else UpdatePolicy.Notify
            viewModelScope.launch { userSettingsRepository.setDefaultUpdatePolicy(policy) }
        }

        fun setAutoUpdateOnlyOnUnmeteredNetwork(onlyOnUnmeteredNetwork: Boolean) {
            viewModelScope.launch { userSettingsRepository.setAutoUpdateOnlyOnUnmeteredNetwork(onlyOnUnmeteredNetwork) }
        }

        fun setAutoUpdateOnlyWhileCharging(onlyWhileCharging: Boolean) {
            viewModelScope.launch { userSettingsRepository.setAutoUpdateOnlyWhileCharging(onlyWhileCharging) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    /**
     * @property account the signed-in GitHub account, `null` when signed out.
     * @property accountNeedsSignIn the saved token expired or was revoked.
     */
    data class Success(
        val settings: UserSettings,
        val account: GitHubAccount? = null,
        val accountNeedsSignIn: Boolean = false,
    ) : SettingsUiState
}
