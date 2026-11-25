package me.geckotv.apkmanager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import me.geckotv.apkmanager.core.designsystem.preview.ShowcaseThemeMode
import me.geckotv.apkmanager.core.designsystem.preview.ShowcaseThemeSettings
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.ui.ApkManagerApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Temporary in-memory theme settings; they move to persisted user settings later.
            var themeMode by rememberSaveable { mutableStateOf(ShowcaseThemeMode.System) }
            var dynamicColor by rememberSaveable { mutableStateOf(true) }
            var amoledBlack by rememberSaveable { mutableStateOf(false) }
            var hapticsEnabled by rememberSaveable { mutableStateOf(true) }

            val darkTheme =
                when (themeMode) {
                    ShowcaseThemeMode.System -> isSystemInDarkTheme()
                    ShowcaseThemeMode.Light -> false
                    ShowcaseThemeMode.Dark -> true
                }

            // Keep the system bar icons readable when the in-app theme differs from the system theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }

            ApkManagerTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                amoledBlack = amoledBlack,
                hapticsEnabled = hapticsEnabled,
            ) {
                ApkManagerApp(
                    settings = ShowcaseThemeSettings(themeMode, dynamicColor, amoledBlack, hapticsEnabled),
                    onSettingsChange = { settings ->
                        themeMode = settings.themeMode
                        dynamicColor = settings.dynamicColor
                        amoledBlack = settings.amoledBlack
                        hapticsEnabled = settings.hapticsEnabled
                    },
                )
            }
        }
    }
}
