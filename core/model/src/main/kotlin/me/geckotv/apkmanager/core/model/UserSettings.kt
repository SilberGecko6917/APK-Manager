package me.geckotv.apkmanager.core.model

/** Settings the user can change in the app. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val useDynamicColor: Boolean = true,
    val useAmoledBlack: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

/** Whether the app follows the system theme or always uses a light or dark theme. */
enum class ThemeMode {
    System,
    Light,
    Dark,
}
