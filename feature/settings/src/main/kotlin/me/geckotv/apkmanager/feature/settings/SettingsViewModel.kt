package me.geckotv.apkmanager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UserSettings
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userSettingsRepository: UserSettingsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            userSettingsRepository.settings
                .map<UserSettings, SettingsUiState>(SettingsUiState::Success)
                .stateIn(
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

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val settings: UserSettings,
    ) : SettingsUiState
}
