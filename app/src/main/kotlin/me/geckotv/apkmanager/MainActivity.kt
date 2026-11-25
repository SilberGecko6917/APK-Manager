package me.geckotv.apkmanager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.ui.ApkManagerApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Keep the splash screen until the settings are loaded, so the first frame has the right theme.
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value is MainActivityUiState.Loading }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val settings = (uiState as? MainActivityUiState.Success)?.settings ?: return@setContent

            val darkTheme = shouldUseDarkTheme(settings)
            SystemBarsEffect(darkTheme)

            ApkManagerTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor,
                amoledBlack = settings.useAmoledBlack,
                hapticsEnabled = settings.hapticsEnabled,
            ) {
                ApkManagerApp(onboardingCompleted = settings.onboardingCompleted)
            }
        }
    }
}

@Composable
private fun shouldUseDarkTheme(settings: UserSettings): Boolean =
    when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

/** Keeps the system bar icons readable when the in-app theme differs from the system theme. */
@Composable
private fun ComponentActivity.SystemBarsEffect(darkTheme: Boolean) {
    DisposableEffect(darkTheme) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
        )
        onDispose {}
    }
}
