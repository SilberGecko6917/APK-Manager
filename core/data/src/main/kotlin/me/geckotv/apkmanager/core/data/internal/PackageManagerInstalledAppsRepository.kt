package me.geckotv.apkmanager.core.data.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.data.repository.InstalledAppsRepository
import me.geckotv.apkmanager.core.model.InstalledApp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [InstalledAppsRepository] backed by [PackageManager] and package broadcasts. Only [installedApp] reads
 * which app installs updates of a package, which takes one more call per package.
 */
@Singleton
internal class PackageManagerInstalledAppsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @Dispatcher(ApkManagerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ) : InstalledAppsRepository {
        private val packageManager: PackageManager get() = context.packageManager

        override val packageChanges: Flow<Unit> =
            callbackFlow {
                val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(
                            context: Context,
                            intent: Intent,
                        ) {
                            trySend(Unit)
                        }
                    }
                val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_PACKAGE_ADDED)
                        addAction(Intent.ACTION_PACKAGE_REMOVED)
                        addAction(Intent.ACTION_PACKAGE_REPLACED)
                        addAction(Intent.ACTION_PACKAGE_CHANGED)
                        addDataScheme("package")
                    }
                // Package broadcasts come from the system, which may send to non-exported receivers.
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                send(Unit)
                awaitClose { context.unregisterReceiver(receiver) }
            }.conflate()
                .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

        override suspend fun installedApp(packageName: String): InstalledApp? =
            withContext(ioDispatcher) {
                try {
                    packageManager
                        .packageInfo(packageName)
                        .toInstalledApp()
                        ?.copy(installerPackageName = packageManager.updateInstaller(packageName))
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }

        override suspend fun installedApps(): List<InstalledApp> =
            withContext(ioDispatcher) {
                packageManager.installedPackages().mapNotNull { it.toInstalledApp() }
            }

        override fun observeInstalledApps(): Flow<List<InstalledApp>> =
            packageChanges
                .mapLatest { installedApps() }
                .flowOn(ioDispatcher)

        private fun PackageInfo.toInstalledApp(): InstalledApp? {
            val applicationInfo = applicationInfo ?: return null
            return InstalledApp(
                packageName = packageName,
                label = applicationInfo.loadLabel(packageManager).toString(),
                versionName = versionName,
                versionCode = PackageInfoCompat.getLongVersionCode(this),
                isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                targetSdkVersion = applicationInfo.targetSdkVersion,
            )
        }

        /**
         * The app that may update [packageName] without asking: its update owner (Android 14+), else the
         * app that installed it.
         */
        private fun PackageManager.updateInstaller(packageName: String): String? =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val source = getInstallSourceInfo(packageName)
                    val owner =
                        if (Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ) {
                            source.updateOwnerPackageName
                        } else {
                            null
                        }
                    owner ?: source.installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    getInstallerPackageName(packageName)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }

        private fun PackageManager.packageInfo(packageName: String): PackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                getPackageInfo(packageName, 0)
            }

        private fun PackageManager.installedPackages(): List<PackageInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                getInstalledPackages(0)
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
