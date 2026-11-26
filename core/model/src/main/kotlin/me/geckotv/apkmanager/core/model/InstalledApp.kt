package me.geckotv.apkmanager.core.model

/** An app installed on the device. */
data class InstalledApp(
    val packageName: String,
    /** The app's name as shown in the launcher. */
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    /** Whether the app is part of the system image (including updated system apps). */
    val isSystemApp: Boolean,
    /** The Android API level the app targets. */
    val targetSdkVersion: Int = 0,
    /**
     * Package name of the app that installs updates of this app without asking: the update owner
     * (Android 14+) or else the app that installed it. `null` if unknown.
     */
    val installerPackageName: String? = null,
)
