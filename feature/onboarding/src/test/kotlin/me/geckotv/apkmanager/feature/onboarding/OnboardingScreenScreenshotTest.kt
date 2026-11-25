package me.geckotv.apkmanager.feature.onboarding

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Record with `./gradlew :feature:onboarding:recordRoborazziDebug`. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class OnboardingScreenScreenshotTest {
    @Test
    fun onboardingLight() =
        capture("onboarding_light", darkTheme = false) {
            OnboardingScreen(
                installPermissionGranted = false,
                notificationPermissionGranted = false,
                onRequestInstallPermission = {},
                onRequestNotificationPermission = {},
                onFinish = {},
            )
        }

    @Test
    fun onboardingDark() =
        capture("onboarding_dark", darkTheme = true) {
            OnboardingScreen(
                installPermissionGranted = true,
                notificationPermissionGranted = false,
                onRequestInstallPermission = {},
                onRequestNotificationPermission = {},
                onFinish = {},
            )
        }

    private fun capture(
        name: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name.png") {
            ApkManagerTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
        }
    }
}
