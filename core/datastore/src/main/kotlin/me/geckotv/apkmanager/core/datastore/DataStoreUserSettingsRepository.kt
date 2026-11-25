package me.geckotv.apkmanager.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UserSettings
import java.io.IOException
import javax.inject.Inject

/** [UserSettingsRepository] backed by a Preferences [DataStore]. */
internal class DataStoreUserSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserSettingsRepository {
        override val settings: Flow<UserSettings> =
            dataStore.data
                .catch { error ->
                    // A corrupt or unreadable file falls back to the defaults instead of crashing.
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { preferences -> preferences.toUserSettings() }
                .distinctUntilChanged()

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
        }

        override suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
            dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = useDynamicColor }
        }

        override suspend fun setUseAmoledBlack(useAmoledBlack: Boolean) {
            dataStore.edit { it[Keys.USE_AMOLED_BLACK] = useAmoledBlack }
        }

        override suspend fun setHapticsEnabled(hapticsEnabled: Boolean) {
            dataStore.edit { it[Keys.HAPTICS_ENABLED] = hapticsEnabled }
        }

        override suspend fun setOnboardingCompleted(onboardingCompleted: Boolean) {
            dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = onboardingCompleted }
        }

        private fun Preferences.toUserSettings(): UserSettings {
            val defaults = UserSettings()
            return UserSettings(
                themeMode =
                    this[Keys.THEME_MODE]
                        ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                        ?: defaults.themeMode,
                useDynamicColor = this[Keys.USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                useAmoledBlack = this[Keys.USE_AMOLED_BLACK] ?: defaults.useAmoledBlack,
                hapticsEnabled = this[Keys.HAPTICS_ENABLED] ?: defaults.hapticsEnabled,
                onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            )
        }

        private object Keys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
            val USE_AMOLED_BLACK = booleanPreferencesKey("use_amoled_black")
            val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        }
    }
