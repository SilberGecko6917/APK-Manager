package me.geckotv.apkmanager.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.geckotv.apkmanager.core.data.internal.AndroidDeviceInfo
import me.geckotv.apkmanager.core.data.internal.OfflineFirstTrackedAppsRepository
import me.geckotv.apkmanager.core.data.internal.PackageManagerInstalledAppsRepository
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindsTrackedAppsRepository(repository: OfflineFirstTrackedAppsRepository): TrackedAppsRepository

    @Binds
    fun bindsInstalledAppsRepository(repository: PackageManagerInstalledAppsRepository): InstalledAppsRepository

    @Binds
    fun bindsDeviceInfo(deviceInfo: AndroidDeviceInfo): DeviceInfo
}
