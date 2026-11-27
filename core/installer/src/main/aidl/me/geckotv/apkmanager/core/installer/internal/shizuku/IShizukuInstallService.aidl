package me.geckotv.apkmanager.core.installer.internal.shizuku;

/**
 * Runs `pm install` with Shizuku's privileges (ADB or root). APK Manager streams the APK in chunks,
 * so the service never needs access to APK Manager's files.
 */
interface IShizukuInstallService {
    /** Called by Shizuku when the service is unbound with remove = true; ends the process. */
    void destroy() = 16777114;

    /** Starts `pm install` for an APK of [size] bytes, with [installerPackageName] as the installer of record. */
    void begin(long size, String installerPackageName) = 1;

    /** Writes the next part of the APK. */
    void write(in byte[] chunk) = 2;

    /** Ends the APK, waits for `pm` and returns its output ("Success" or "Failure [...]"). */
    String finish() = 3;

    /** Stops a running install. */
    void abort() = 4;
}
