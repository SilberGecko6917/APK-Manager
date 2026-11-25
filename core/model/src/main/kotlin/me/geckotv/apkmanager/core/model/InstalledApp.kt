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
)
