package me.geckotv.apkmanager.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserSettingsRepositoryTest {
    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val repository =
        DataStoreUserSettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = testScope.backgroundScope,
                produceFile = { tmpFolder.newFile("user_settings_test.preferences_pb") },
            ),
        )

    @Test
    fun emitsDefaultsWhenNothingIsStored() =
        testScope.runTest {
            repository.settings.test {
                assertEquals(UserSettings(), awaitItem())
            }
        }

    @Test
    fun persistsEverySetting() =
        testScope.runTest {
            repository.setThemeMode(ThemeMode.Dark)
            repository.setUseDynamicColor(false)
            repository.setUseAmoledBlack(true)
            repository.setHapticsEnabled(false)
            repository.setOnboardingCompleted(true)
            repository.setSelfUpdatesTracked(true)
            repository.setBackgroundChecksEnabled(false)
            repository.setCheckInterval(UpdateCheckInterval.Every24Hours)
            repository.setCheckOnlyOnUnmeteredNetwork(true)
            repository.setDefaultUpdatePolicy(UpdatePolicy.Install)
            repository.setAutoUpdateOnlyOnUnmeteredNetwork(false)
            repository.setAutoUpdateOnlyWhileCharging(true)

            repository.settings.test {
                assertEquals(
                    UserSettings(
                        themeMode = ThemeMode.Dark,
                        useDynamicColor = false,
                        useAmoledBlack = true,
                        hapticsEnabled = false,
                        onboardingCompleted = true,
                        selfUpdatesTracked = true,
                        backgroundChecksEnabled = false,
                        checkInterval = UpdateCheckInterval.Every24Hours,
                        checkOnlyOnUnmeteredNetwork = true,
                        defaultUpdatePolicy = UpdatePolicy.Install,
                        autoUpdateOnlyOnUnmeteredNetwork = false,
                        autoUpdateOnlyWhileCharging = true,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun emitsAgainWhenASettingChanges() =
        testScope.runTest {
            repository.settings.test {
                assertEquals(ThemeMode.System, awaitItem().themeMode)
                repository.setThemeMode(ThemeMode.Light)
                assertEquals(ThemeMode.Light, awaitItem().themeMode)
            }
        }
}
