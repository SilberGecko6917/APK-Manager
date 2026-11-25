package me.geckotv.apkmanager.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.model.InstalledApp

class FakeInstalledAppsRepository : InstalledAppsRepository {
    private val apps = MutableStateFlow<Map<String, InstalledApp>>(emptyMap())

    override val packageChanges: Flow<Unit> = apps.map { }

    fun install(app: InstalledApp) {
        apps.value += app.packageName to app
    }

    override suspend fun installedApp(packageName: String): InstalledApp? = apps.value[packageName]

    override suspend fun installedApps(): List<InstalledApp> = apps.value.values.toList()

    override fun observeInstalledApps(): Flow<List<InstalledApp>> = apps.map { it.values.toList() }
}
