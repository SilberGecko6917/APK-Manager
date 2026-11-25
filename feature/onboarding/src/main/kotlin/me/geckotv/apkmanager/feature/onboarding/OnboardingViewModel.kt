package me.geckotv.apkmanager.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val userSettingsRepository: UserSettingsRepository,
    ) : ViewModel() {
        /** Marks onboarding as completed; the app observes the setting and leaves onboarding. */
        fun completeOnboarding() {
            viewModelScope.launch {
                userSettingsRepository.setOnboardingCompleted(true)
            }
        }
    }
