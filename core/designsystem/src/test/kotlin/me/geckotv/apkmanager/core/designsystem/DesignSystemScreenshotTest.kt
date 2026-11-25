package me.geckotv.apkmanager.core.designsystem

import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.preview.DesignSystemShowcase
import me.geckotv.apkmanager.core.designsystem.preview.ShowcaseThemeMode
import me.geckotv.apkmanager.core.designsystem.preview.ShowcaseThemeSettings
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.designsystem.theme.ColorStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the [DesignSystemShowcase] in different theme configurations.
 *
 * Record the images with `./gradlew :core:designsystem:recordRoborazziDebug`; they are written to
 * `core/designsystem/build/outputs/roborazzi/`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class DesignSystemScreenshotTest {
    @Test
    fun showcaseLight() = capture(name = "showcase_light", darkTheme = false)

    @Test
    fun showcaseDark() = capture(name = "showcase_dark", darkTheme = true)

    @Test
    fun showcaseAmoled() = capture(name = "showcase_amoled", darkTheme = true, amoledBlack = true)

    @Test
    fun showcaseExpressiveStyle() =
        capture(name = "showcase_expressive", darkTheme = false, colorStyle = ColorStyle.Expressive)

    private fun capture(
        name: String,
        darkTheme: Boolean,
        amoledBlack: Boolean = false,
        colorStyle: ColorStyle = ColorStyle.TonalSpot,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(
                darkTheme = darkTheme,
                dynamicColor = false,
                colorStyle = colorStyle,
                amoledBlack = amoledBlack,
            ) {
                DesignSystemShowcase(
                    settings =
                        ShowcaseThemeSettings(
                            themeMode = if (darkTheme) ShowcaseThemeMode.Dark else ShowcaseThemeMode.Light,
                            dynamicColor = false,
                            amoledBlack = amoledBlack,
                        ),
                    onSettingsChange = {},
                )
            }
        }
    }
}
