package me.geckotv.apkmanager.feature.apps

import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class AppsScreenScreenshotTest {
    @Test
    fun appsEmptyLight() = capture("apps_empty_light", darkTheme = false)

    @Test
    fun appsEmptyDark() = capture("apps_empty_dark", darkTheme = true)

    private fun capture(
        name: String,
        darkTheme: Boolean,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(darkTheme = darkTheme, dynamicColor = false) {
                AppsScreen()
            }
        }
    }
}
