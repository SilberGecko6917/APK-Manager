package me.geckotv.apkmanager.feature.onboarding

import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserSettingsRepository()
    private val viewModel = OnboardingViewModel(repository)

    @Test
    fun completeOnboarding_marksOnboardingAsCompleted() {
        assertFalse(repository.current.onboardingCompleted)

        viewModel.completeOnboarding()

        assertTrue(repository.current.onboardingCompleted)
    }
}
