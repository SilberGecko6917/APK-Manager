package me.geckotv.apkmanager.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.materialkolor.rememberDynamicColorScheme

/**
 * The APK Manager theme: Material 3 with expressive motion, rounded typography and larger shapes.
 *
 * @param darkTheme whether to use a dark color scheme.
 * @param dynamicColor whether to use the wallpaper-based Material You colors (Android 12+). Falls
 *   back to [seedColor] on older versions or when turned off.
 * @param seedColor the color a scheme is generated from when [dynamicColor] is not used.
 * @param colorStyle how the scheme is derived from [seedColor].
 * @param amoledBlack whether dark themes use a pitch black background.
 * @param hapticsEnabled whether components perform haptic feedback.
 */
@Composable
fun ApkManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = DefaultSeedColor,
    colorStyle: ColorStyle = ColorStyle.TonalSpot,
    amoledBlack: Boolean = false,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        rememberApkManagerColorScheme(
            darkTheme = darkTheme,
            dynamicColor = dynamicColor,
            seedColor = seedColor,
            colorStyle = colorStyle,
            amoledBlack = amoledBlack,
        )
    val systemHaptics = LocalHapticFeedback.current
    val haptics = remember(systemHaptics, hapticsEnabled) { if (hapticsEnabled) systemHaptics else NoOpHapticFeedback }

    MaterialTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ApkManagerShapes,
        typography = ApkManagerTypography,
    ) {
        CompositionLocalProvider(LocalHapticFeedback provides haptics, content = content)
    }
}

@Composable
private fun rememberApkManagerColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    seedColor: Color,
    colorStyle: ColorStyle,
    amoledBlack: Boolean,
): ColorScheme {
    val amoled = darkTheme && amoledBlack
    return if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        remember(context, darkTheme, amoled) {
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (amoled) scheme.toAmoled() else scheme
        }
    } else {
        val scheme =
            rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = darkTheme,
                isAmoled = false,
                style = colorStyle.paletteStyle,
            )
        remember(scheme, amoled) { if (amoled) scheme.toAmoled() else scheme }
    }
}
