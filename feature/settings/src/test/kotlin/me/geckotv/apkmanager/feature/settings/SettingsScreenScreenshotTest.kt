package me.geckotv.apkmanager.feature.settings

import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UserSettings
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

    private fun capture(
        name: String,
        settings: UserSettings,
        darkTheme: Boolean,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(darkTheme = darkTheme, dynamicColor = false, amoledBlack = settings.useAmoledBlack) {
                SettingsScreen(
                    uiState = SettingsUiState.Success(settings),
                    versionName = "0.1.0",
                    supportsDynamicColor = true,
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                    onAmoledBlackChange = {},
                    onHapticsChange = {},
                    onOpenSourceCode = {},
                    onOpenLicense = {},
                )
            }
        }
    }
}
