package me.geckotv.apkmanager.core.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.model.InstallerBackend
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings

/** In-memory [UserSettingsRepository] for tests. */
class FakeUserSettingsRepository(
    initial: UserSettings = UserSettings(),
) : UserSettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state

    /** The current value, for assertions. */
    val current: UserSettings get() = state.value

    override suspend fun setThemeMode(themeMode: ThemeMode) = state.update { it.copy(themeMode = themeMode) }

    override suspend fun setUseDynamicColor(useDynamicColor: Boolean) =
        state.update { it.copy(useDynamicColor = useDynamicColor) }

    override suspend fun setUseAmoledBlack(useAmoledBlack: Boolean) =
        state.update { it.copy(useAmoledBlack = useAmoledBlack) }

    override suspend fun setHapticsEnabled(hapticsEnabled: Boolean) =
        state.update { it.copy(hapticsEnabled = hapticsEnabled) }

    override suspend fun setOnboardingCompleted(onboardingCompleted: Boolean) =
        state.update { it.copy(onboardingCompleted = onboardingCompleted) }

    override suspend fun setSelfUpdatesTracked(selfUpdatesTracked: Boolean) =
        state.update { it.copy(selfUpdatesTracked = selfUpdatesTracked) }

    override suspend fun setBackgroundChecksEnabled(backgroundChecksEnabled: Boolean) =
        state.update { it.copy(backgroundChecksEnabled = backgroundChecksEnabled) }

    override suspend fun setCheckInterval(checkInterval: UpdateCheckInterval) =
        state.update { it.copy(checkInterval = checkInterval) }

    override suspend fun setCheckOnlyOnUnmeteredNetwork(checkOnlyOnUnmeteredNetwork: Boolean) =
        state.update { it.copy(checkOnlyOnUnmeteredNetwork = checkOnlyOnUnmeteredNetwork) }

    override suspend fun setDefaultUpdatePolicy(defaultUpdatePolicy: UpdatePolicy) =
        state.update { it.copy(defaultUpdatePolicy = defaultUpdatePolicy) }

    override suspend fun setAutoUpdateOnlyOnUnmeteredNetwork(autoUpdateOnlyOnUnmeteredNetwork: Boolean) =
        state.update { it.copy(autoUpdateOnlyOnUnmeteredNetwork = autoUpdateOnlyOnUnmeteredNetwork) }

    override suspend fun setAutoUpdateOnlyWhileCharging(autoUpdateOnlyWhileCharging: Boolean) =
        state.update { it.copy(autoUpdateOnlyWhileCharging = autoUpdateOnlyWhileCharging) }

    override suspend fun setInstallerBackend(installerBackend: InstallerBackend) =
        state.update { it.copy(installerBackend = installerBackend) }
}
