package me.geckotv.apkmanager.core.data.repository

import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.model.InstalledApp

/** Apps installed on the device. */
interface InstalledAppsRepository {
    /** Emits once when collected and again whenever an app is installed, updated or removed. */
    val packageChanges: Flow<Unit>

    /** The installed app with [packageName], or `null` if it is not installed. */
    suspend fun installedApp(packageName: String): InstalledApp?

    /** All installed apps, including system apps. */
    suspend fun installedApps(): List<InstalledApp>

    /** [installedApps], updated on package changes. */
    fun observeInstalledApps(): Flow<List<InstalledApp>>
}
