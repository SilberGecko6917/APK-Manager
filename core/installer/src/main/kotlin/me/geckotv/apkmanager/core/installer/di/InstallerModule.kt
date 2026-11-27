package me.geckotv.apkmanager.core.installer.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallerBackends
import me.geckotv.apkmanager.core.installer.internal.AndroidInstallNotifier
import me.geckotv.apkmanager.core.installer.internal.AndroidInstallServiceController
import me.geckotv.apkmanager.core.installer.internal.ApkDownloader
import me.geckotv.apkmanager.core.installer.internal.AssetDownloader
import me.geckotv.apkmanager.core.installer.internal.Backend
import me.geckotv.apkmanager.core.installer.internal.DefaultAppInstaller
import me.geckotv.apkmanager.core.installer.internal.DownloadDirectory
import me.geckotv.apkmanager.core.installer.internal.InstallNotifier
import me.geckotv.apkmanager.core.installer.internal.InstallServiceController
import me.geckotv.apkmanager.core.installer.internal.PackageInspector
import me.geckotv.apkmanager.core.installer.internal.PackageInstallerSystemInstaller
import me.geckotv.apkmanager.core.installer.internal.PackageManagerInspector
import me.geckotv.apkmanager.core.installer.internal.RoutingSystemInstaller
import me.geckotv.apkmanager.core.installer.internal.SystemInstaller
import me.geckotv.apkmanager.core.installer.internal.shizuku.ShizukuInstallerBackends
import me.geckotv.apkmanager.core.installer.internal.shizuku.ShizukuSystemInstaller
import me.geckotv.apkmanager.core.model.InstallerBackend
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
internal object InstallerProvidersModule {
    @Provides
    @DownloadDirectory
    fun providesDownloadDirectory(
        @ApplicationContext context: Context,
    ): File = File(context.cacheDir, "downloads")
}

@Module
@InstallIn(SingletonComponent::class)
internal interface InstallerModule {
    @Binds
    fun bindsAppInstaller(installer: DefaultAppInstaller): AppInstaller

    @Binds
    fun bindsAssetDownloader(downloader: ApkDownloader): AssetDownloader

    @Binds
    fun bindsPackageInspector(inspector: PackageManagerInspector): PackageInspector

    @Binds
    fun bindsSystemInstaller(installer: RoutingSystemInstaller): SystemInstaller

    @Binds
    @Backend(InstallerBackend.System)
    fun bindsPackageInstallerSystemInstaller(installer: PackageInstallerSystemInstaller): SystemInstaller

    @Binds
    @Backend(InstallerBackend.Shizuku)
    fun bindsShizukuSystemInstaller(installer: ShizukuSystemInstaller): SystemInstaller

    @Binds
    fun bindsInstallerBackends(backends: ShizukuInstallerBackends): InstallerBackends

    @Binds
    fun bindsInstallNotifier(notifier: AndroidInstallNotifier): InstallNotifier

    @Binds
    fun bindsInstallServiceController(controller: AndroidInstallServiceController): InstallServiceController
}
