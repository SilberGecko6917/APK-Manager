package me.geckotv.apkmanager.core.model

/**
 * Whether Android installs an update without asking the user (`USER_ACTION_NOT_REQUIRED`, Android 12+).
 * Android still may ask, so installers must always handle a confirmation.
 */
object UnattendedUpdates {
    /** First Android version (12, API 31) that installs updates without asking. */
    private const val FIRST_SDK = 31

    /**
     * Whether Android on API level [sdkInt] can update [app] without asking when APK Manager
     * ([ownPackageName]) installs it: APK Manager installed the app (or owns its updates) or it is
     * APK Manager itself, and the app targets a recent enough API level.
     */
    fun isPossible(
        app: InstalledApp,
        ownPackageName: String,
        sdkInt: Int,
    ): Boolean {
        if (sdkInt < FIRST_SDK) return false
        val ownsUpdates = app.packageName == ownPackageName || app.installerPackageName == ownPackageName
        return ownsUpdates && app.targetSdkVersion >= minimumTargetSdk(sdkInt)
    }

    /**
     * The lowest target API level of an app that Android on API level [sdkInt] updates without asking,
     * see `PackageInstaller.SessionParams.setRequireUserAction`.
     */
    fun minimumTargetSdk(sdkInt: Int): Int =
        when {
            sdkInt <= ANDROID_12L -> ANDROID_10

            sdkInt == ANDROID_13 -> ANDROID_11

            sdkInt == ANDROID_14 -> ANDROID_12

            // From Android 15 on, apps must target at most two versions back.
            else -> sdkInt - 2
        }

    private const val ANDROID_10 = 29
    private const val ANDROID_11 = 30
    private const val ANDROID_12 = 31
    private const val ANDROID_12L = 32
    private const val ANDROID_13 = 33
    private const val ANDROID_14 = 34
}
