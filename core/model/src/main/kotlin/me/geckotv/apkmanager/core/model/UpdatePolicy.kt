package me.geckotv.apkmanager.core.model

/** What APK Manager does when a background check finds an update of an app. */
enum class UpdatePolicy {
    /** Show a notification; the user installs the update. */
    Notify,

    /**
     * Download, verify and install the update. Android installs it without asking if APK Manager
     * installed the app; otherwise a notification asks the user to confirm the installation.
     */
    Install,
}
