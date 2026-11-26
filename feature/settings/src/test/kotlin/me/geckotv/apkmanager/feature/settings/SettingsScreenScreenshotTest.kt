package me.geckotv.apkmanager.feature.settings

import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.model.InstallerBackend
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeAuthRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsScreenScreenshotTest {
    @Test
    fun settingsLight() = capture("settings_light", UserSettings(themeMode = ThemeMode.Light), darkTheme = false)

    @Test
    fun settingsDark() =
        capture(
            "settings_dark",
            UserSettings(themeMode = ThemeMode.Dark, useAmoledBlack = true),
            darkTheme = true,
        )

    @Test
    fun settingsSignedInLight() =
        capture(
            "settings_signed_in_light",
            UserSettings(themeMode = ThemeMode.Light),
            darkTheme = false,
            account = FakeAuthRepository.sampleAccount(),
        )

    @Test
    fun settingsAutomaticUpdatesLight() =
        capture(
            "settings_automatic_updates_light",
            UserSettings(
                themeMode = ThemeMode.Light,
                defaultUpdatePolicy = UpdatePolicy.Install,
                autoUpdateOnlyWhileCharging = true,
            ),
            darkTheme = false,
        )

    // A taller screen, so the "Installer" section below the automatic updates is visible.
    @Test
    @Config(qualifiers = "w411dp-h1400dp-xxhdpi")
    fun settingsShizukuPermissionRequiredLight() =
        capture(
            "settings_shizuku_permission_required_light",
            UserSettings(themeMode = ThemeMode.Light, installerBackend = InstallerBackend.Shizuku),
            darkTheme = false,
            shizukuState = ShizukuState.PermissionRequired,
        )

    @Test
    @Config(qualifiers = "w411dp-h1400dp-xxhdpi")
    fun settingsShizukuReadyDark() =
        capture(
            "settings_shizuku_ready_dark",
            UserSettings(themeMode = ThemeMode.Dark, installerBackend = InstallerBackend.Shizuku),
            darkTheme = true,
            shizukuState = ShizukuState.Ready(isRoot = false),
        )

    private fun capture(
        name: String,
        settings: UserSettings,
        darkTheme: Boolean,
        account: GitHubAccount? = null,
        shizukuState: ShizukuState = ShizukuState.NotInstalled,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(darkTheme = darkTheme, dynamicColor = false, amoledBlack = settings.useAmoledBlack) {
                SettingsScreen(
                    uiState = SettingsUiState.Success(settings, account = account, shizukuState = shizukuState),
                    versionName = "0.1.0",
                    supportsDynamicColor = true,
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                    onAmoledBlackChange = {},
                    onHapticsChange = {},
                    onBackgroundChecksChange = {},
                    onCheckIntervalChange = {},
                    onUnmeteredOnlyChange = {},
                    automaticUpdates = AutomaticUpdatesActions(),
                    installer = InstallerActions(),
                    onOpenSourceCode = {},
                    onOpenLicense = {},
                    onOpenAccount = {},
                )
            }
        }
    }
}
