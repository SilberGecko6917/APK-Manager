package me.geckotv.apkmanager.core.data.repository

/** Properties of the device that decide which APK fits. */
interface DeviceInfo {
    /** ABIs the device supports, most preferred first (`Build.SUPPORTED_ABIS`). */
    val supportedAbis: List<String>

    /** The Android API level of the device (`Build.VERSION.SDK_INT`). */
    val sdkInt: Int

    /** The package name of APK Manager itself. */
    val ownPackageName: String
}
