package me.geckotv.apkmanager.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import me.geckotv.apkmanager.core.designsystem.R

/** Value of the variable font's `ROND` (roundness) axis, 0 to 100. */
private const val ROUNDNESS = 100f

private fun roundedGoogleSansFlex(weight: FontWeight) =
    Font(
        resId = R.font.core_designsystem_google_sans_flex,
        weight = weight,
        variationSettings =
            FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("ROND", ROUNDNESS),
            ),
    )

/** Google Sans Flex with fully rounded terminals. */
val GoogleSansFlexRounded: FontFamily =
    FontFamily(
        roundedGoogleSansFlex(FontWeight.Normal),
        roundedGoogleSansFlex(FontWeight.Medium),
        roundedGoogleSansFlex(FontWeight.SemiBold),
        roundedGoogleSansFlex(FontWeight.Bold),
    )

internal val ApkManagerTypography: Typography = Typography().withFontFamily(GoogleSansFlexRounded)

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography =
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
        displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = fontFamily),
        displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = fontFamily),
        displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = fontFamily),
        headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
        headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
        headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
        titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
        titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
        titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
        bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
        bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
        bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
        labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
        labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
        labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily),
    )
