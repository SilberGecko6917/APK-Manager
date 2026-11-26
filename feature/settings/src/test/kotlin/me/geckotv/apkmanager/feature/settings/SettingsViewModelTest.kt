package me.geckotv.apkmanager.feature.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import me.geckotv.apkmanager.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserSettingsRepository()
    private lateinit var viewModel: SettingsViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = SettingsViewModel(repository)
    }

    @Test
    fun uiStateReflectsStoredSettings() =
        runTest {
            // uiState only collects its upstream while it has a subscriber.
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

            assertEquals(SettingsUiState.Success(UserSettings()), viewModel.uiState.value)

            repository.setThemeMode(ThemeMode.Light)
            assertEquals(SettingsUiState.Success(UserSettings(themeMode = ThemeMode.Light)), viewModel.uiState.value)
        }

    @Test
    fun changesAreWrittenToTheRepository() =
        runTest {
            viewModel.setThemeMode(ThemeMode.Dark)
            viewModel.setUseDynamicColor(false)
            viewModel.setUseAmoledBlack(true)
            viewModel.setHapticsEnabled(false)
            viewModel.setBackgroundChecksEnabled(false)
            viewModel.setCheckInterval(UpdateCheckInterval.Every6Hours)
            viewModel.setCheckOnlyOnUnmeteredNetwork(true)

            assertEquals(
                UserSettings(
                    themeMode = ThemeMode.Dark,
                    useDynamicColor = false,
                    useAmoledBlack = true,
                    hapticsEnabled = false,
                    backgroundChecksEnabled = false,
                    checkInterval = UpdateCheckInterval.Every6Hours,
                    checkOnlyOnUnmeteredNetwork = true,
                ),
                repository.current,
            )
        }
}
