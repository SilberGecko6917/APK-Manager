# Shizuku creates the install service by class name in its own process.
-keep class me.geckotv.apkmanager.core.installer.internal.shizuku.ShizukuInstallService {
    <init>(...);
}
