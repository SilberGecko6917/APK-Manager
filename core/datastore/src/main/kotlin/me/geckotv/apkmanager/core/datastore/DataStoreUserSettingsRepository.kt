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
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
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

        override suspend fun setSelfUpdatesTracked(selfUpdatesTracked: Boolean) {
            dataStore.edit { it[Keys.SELF_UPDATES_TRACKED] = selfUpdatesTracked }
        }

        override suspend fun setBackgroundChecksEnabled(backgroundChecksEnabled: Boolean) {
            dataStore.edit { it[Keys.BACKGROUND_CHECKS_ENABLED] = backgroundChecksEnabled }
        }

        override suspend fun setCheckInterval(checkInterval: UpdateCheckInterval) {
            dataStore.edit { it[Keys.CHECK_INTERVAL] = checkInterval.name }
        }

        override suspend fun setCheckOnlyOnUnmeteredNetwork(checkOnlyOnUnmeteredNetwork: Boolean) {
            dataStore.edit { it[Keys.CHECK_ONLY_ON_UNMETERED_NETWORK] = checkOnlyOnUnmeteredNetwork }
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
                selfUpdatesTracked = this[Keys.SELF_UPDATES_TRACKED] ?: defaults.selfUpdatesTracked,
                backgroundChecksEnabled = this[Keys.BACKGROUND_CHECKS_ENABLED] ?: defaults.backgroundChecksEnabled,
                checkInterval =
                    this[Keys.CHECK_INTERVAL]
                        ?.let { name -> UpdateCheckInterval.entries.firstOrNull { it.name == name } }
                        ?: defaults.checkInterval,
                checkOnlyOnUnmeteredNetwork =
                    this[Keys.CHECK_ONLY_ON_UNMETERED_NETWORK] ?: defaults.checkOnlyOnUnmeteredNetwork,
            )
        }

        private object Keys {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
            val USE_AMOLED_BLACK = booleanPreferencesKey("use_amoled_black")
            val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
            val SELF_UPDATES_TRACKED = booleanPreferencesKey("self_updates_tracked")
            val BACKGROUND_CHECKS_ENABLED = booleanPreferencesKey("background_checks_enabled")
            val CHECK_INTERVAL = stringPreferencesKey("check_interval")
            val CHECK_ONLY_ON_UNMETERED_NETWORK = booleanPreferencesKey("check_only_on_unmetered_network")
        }
    }
