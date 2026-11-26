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
import me.geckotv.apkmanager.core.installer.InstallerBackends
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.model.InstallerBackend
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings
import javax.inject.Inject

/** One function per setting; splitting it would only scatter them. */
@Suppress("TooManyFunctions")
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userSettingsRepository: UserSettingsRepository,
        authRepository: AuthRepository,
        private val installerBackends: InstallerBackends,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                userSettingsRepository.settings,
                authRepository.authState,
                installerBackends.shizukuState,
            ) { settings, authState, shizukuState ->
                SettingsUiState.Success(
                    settings = settings,
                    shizukuState = shizukuState,
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

        fun setInstallerBackend(installerBackend: InstallerBackend) {
            viewModelScope.launch { userSettingsRepository.setInstallerBackend(installerBackend) }
        }

        /** Asks Shizuku for permission; the answer arrives in [SettingsUiState.Success.shizukuState]. */
        fun requestShizukuPermission() {
            installerBackends.requestShizukuPermission()
        }

        /** Checks Shizuku again, e.g. when the user comes back from the Shizuku app. */
        fun refreshShizukuState() {
            installerBackends.refreshShizukuState()
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
     * @property shizukuState whether Shizuku can install apps; only matters while it is the selected installer.
     */
    data class Success(
        val settings: UserSettings,
        val account: GitHubAccount? = null,
        val accountNeedsSignIn: Boolean = false,
        val shizukuState: ShizukuState = ShizukuState.NotInstalled,
    ) : SettingsUiState
}
