package me.geckotv.apkmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.model.UserSettings
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel
    @Inject
    constructor(
        userSettingsRepository: UserSettingsRepository,
    ) : ViewModel() {
        val uiState: StateFlow<MainActivityUiState> =
            userSettingsRepository.settings
                .map<UserSettings, MainActivityUiState>(MainActivityUiState::Success)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = MainActivityUiState.Loading,
                )

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

sealed interface MainActivityUiState {
    /** Settings are not loaded yet; the splash screen stays visible. */
    data object Loading : MainActivityUiState

    data class Success(
        val settings: UserSettings,
    ) : MainActivityUiState
}
