package me.geckotv.apkmanager.core.datastore

import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings

/** Reads and writes the [UserSettings]. */
@Suppress("TooManyFunctions") // One setter per setting.
interface UserSettingsRepository {
    /** The current settings; emits again whenever a setting changes. */
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(useDynamicColor: Boolean)

    suspend fun setUseAmoledBlack(useAmoledBlack: Boolean)

    suspend fun setHapticsEnabled(hapticsEnabled: Boolean)

    suspend fun setOnboardingCompleted(onboardingCompleted: Boolean)

    suspend fun setSelfUpdatesTracked(selfUpdatesTracked: Boolean)

    suspend fun setBackgroundChecksEnabled(backgroundChecksEnabled: Boolean)

    suspend fun setCheckInterval(checkInterval: UpdateCheckInterval)

    suspend fun setCheckOnlyOnUnmeteredNetwork(checkOnlyOnUnmeteredNetwork: Boolean)

    suspend fun setDefaultUpdatePolicy(defaultUpdatePolicy: UpdatePolicy)

    suspend fun setAutoUpdateOnlyOnUnmeteredNetwork(autoUpdateOnlyOnUnmeteredNetwork: Boolean)

    suspend fun setAutoUpdateOnlyWhileCharging(autoUpdateOnlyWhileCharging: Boolean)
}
