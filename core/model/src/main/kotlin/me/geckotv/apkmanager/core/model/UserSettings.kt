package me.geckotv.apkmanager.core.model

/** Settings the user can change in the app. */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val useDynamicColor: Boolean = true,
    val useAmoledBlack: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    /**
     * Whether APK Manager added its own repository to the tracked apps. It does so once, so the user
     * can remove it for good.
     */
    val selfUpdatesTracked: Boolean = false,
    /** Whether APK Manager checks for updates in the background and notifies about them. */
    val backgroundChecksEnabled: Boolean = true,
    val checkInterval: UpdateCheckInterval = UpdateCheckInterval.Every12Hours,
    /** Whether background checks only run on unmetered networks (usually Wi-Fi). */
    val checkOnlyOnUnmeteredNetwork: Boolean = false,
    /** What to do with updates of apps that have no [TrackedApp.updatePolicy] of their own. */
    val defaultUpdatePolicy: UpdatePolicy = UpdatePolicy.Notify,
    /** Whether automatic updates wait for an unmetered network (usually Wi-Fi). */
    val autoUpdateOnlyOnUnmeteredNetwork: Boolean = true,
    /** Whether automatic updates wait until the device is charging. */
    val autoUpdateOnlyWhileCharging: Boolean = false,
    /** How APKs are installed. */
    val installerBackend: InstallerBackend = InstallerBackend.System,
)

/** How often APK Manager checks for updates in the background. */
enum class UpdateCheckInterval(
    val hours: Long,
) {
    Every6Hours(6),
    Every12Hours(12),
    Every24Hours(24),
}

/** Whether the app follows the system theme or always uses a light or dark theme. */
enum class ThemeMode {
    System,
    Light,
    Dark,
}
