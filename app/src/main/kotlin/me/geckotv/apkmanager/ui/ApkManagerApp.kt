package me.geckotv.apkmanager.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.geckotv.apkmanager.core.designsystem.preview.DesignSystemShowcase
import me.geckotv.apkmanager.core.designsystem.preview.ShowcaseThemeSettings

/**
 * Root composable of the app.
 *
 * Until the navigation shell exists, the app shows the design system showcase. Its switches
 * control the real theme, so the design can be tried out on a device.
 */
@Composable
fun ApkManagerApp(
    settings: ShowcaseThemeSettings,
    onSettingsChange: (ShowcaseThemeSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignSystemShowcase(settings = settings, onSettingsChange = onSettingsChange, modifier = modifier)
}
