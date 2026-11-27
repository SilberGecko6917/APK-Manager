package me.geckotv.apkmanager.core.model

/** How APK Manager installs APKs. */
enum class InstallerBackend {
    /**
     * Android's package installer. Android asks the user to confirm installs, except for updates of
     * apps APK Manager installed (Android 12+).
     */
    System,

    /**
     * [Shizuku](https://shizuku.rikka.app/) (or Sui) with ADB or root privileges installs every app
     * without asking. Falls back to [System] while Shizuku is not running or not allowed.
     */
    Shizuku,
}
