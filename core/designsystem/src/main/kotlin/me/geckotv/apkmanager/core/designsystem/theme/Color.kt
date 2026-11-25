package me.geckotv.apkmanager.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/** Brand seed color, used when dynamic color is unavailable or turned off. */
val DefaultSeedColor = Color(0xFF1B6B5A)

/** How a color scheme is derived from a seed color. */
enum class ColorStyle(
    internal val paletteStyle: PaletteStyle,
) {
    TonalSpot(PaletteStyle.TonalSpot),
    Neutral(PaletteStyle.Neutral),
    Vibrant(PaletteStyle.Vibrant),
    Expressive(PaletteStyle.Expressive),
    Fidelity(PaletteStyle.Fidelity),
    Content(PaletteStyle.Content),
    Monochrome(PaletteStyle.Monochrome),
}

/**
 * Turns a dark color scheme into a pitch black variant for OLED screens.
 *
 * Only the background and the lowest surface levels become black, so containers such as cards and
 * list items keep enough contrast to stay visible.
 */
internal fun ColorScheme.toAmoled(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
    )
