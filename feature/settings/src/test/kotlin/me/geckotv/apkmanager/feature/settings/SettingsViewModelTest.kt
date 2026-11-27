package me.geckotv.apkmanager.feature.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.InstallerBackend
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeAuthRepository
import me.geckotv.apkmanager.core.testing.FakeInstallerBackends
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
    private val authRepository = FakeAuthRepository()
    private val installerBackends = FakeInstallerBackends()
    private lateinit var viewModel: SettingsViewModel

    // Created after MainDispatcherRule has replaced Dispatchers.Main, so viewModelScope uses the test dispatcher.
    @Before
    fun setUp() {
        viewModel = SettingsViewModel(repository, authRepository, installerBackends)
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
    fun uiStateShowsTheSignedInAccount() =
        runTest {
            val account = FakeAuthRepository.sampleAccount()
            val signedIn =
                SettingsViewModel(repository, FakeAuthRepository(AuthState.SignedIn(account)), installerBackends)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { signedIn.uiState.collect() }

            assertEquals(SettingsUiState.Success(UserSettings(), account = account), signedIn.uiState.value)
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

    @Test
    fun installingUpdatesAutomaticallySetsTheDefaultPolicy() =
        runTest {
            viewModel.setInstallUpdatesAutomatically(true)
            assertEquals(UpdatePolicy.Install, repository.current.defaultUpdatePolicy)

            viewModel.setInstallUpdatesAutomatically(false)
            assertEquals(UpdatePolicy.Notify, repository.current.defaultUpdatePolicy)
        }

    @Test
    fun automaticUpdateConditionsAreWrittenToTheRepository() =
        runTest {
            viewModel.setAutoUpdateOnlyOnUnmeteredNetwork(false)
            viewModel.setAutoUpdateOnlyWhileCharging(true)

            assertEquals(
                UserSettings(autoUpdateOnlyOnUnmeteredNetwork = false, autoUpdateOnlyWhileCharging = true),
                repository.current,
            )
        }

    @Test
    fun selectingTheInstallerBackendIsWrittenToTheRepository() =
        runTest {
            viewModel.setInstallerBackend(InstallerBackend.Shizuku)
            assertEquals(InstallerBackend.Shizuku, repository.current.installerBackend)

            viewModel.setInstallerBackend(InstallerBackend.System)
            assertEquals(InstallerBackend.System, repository.current.installerBackend)
        }

    @Test
    fun uiStateFollowsShizukuState() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
            assertEquals(ShizukuState.NotInstalled, (viewModel.uiState.value as SettingsUiState.Success).shizukuState)

            installerBackends.shizukuState.value = ShizukuState.Ready(isRoot = true)

            assertEquals(
                ShizukuState.Ready(isRoot = true),
                (viewModel.uiState.value as SettingsUiState.Success).shizukuState,
            )
        }

    @Test
    fun requestingShizukuPermissionAsksTheBackends() {
        viewModel.requestShizukuPermission()

        assertEquals(1, installerBackends.permissionRequests)
    }

    @Test
    fun refreshingShizukuChecksTheBackendsAgain() {
        viewModel.refreshShizukuState()
        viewModel.refreshShizukuState()

        assertEquals(2, installerBackends.refreshes)
    }
}
