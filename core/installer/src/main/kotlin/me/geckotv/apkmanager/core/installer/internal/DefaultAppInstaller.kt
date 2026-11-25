package me.geckotv.apkmanager.core.installer.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.data.repository.DeviceInfo
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.ApkAssets
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.TrackedApp
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs installs in the application scope: pick the APK, download it, verify it, hand it to the system
 * installer, and link the tracked app to the installed package.
 */
@Singleton
internal class DefaultAppInstaller
    @Inject
    constructor(
        private val downloader: AssetDownloader,
        private val inspector: PackageInspector,
        private val systemInstaller: SystemInstaller,
        private val trackedAppsRepository: TrackedAppsRepository,
        private val deviceInfo: DeviceInfo,
        @ApplicationScope private val scope: CoroutineScope,
    ) : AppInstaller {
        private val state = MutableStateFlow<Map<Long, InstallState>>(emptyMap())
        private val jobs = ConcurrentHashMap<Long, Job>()

        override val installs: StateFlow<Map<Long, InstallState>> = state

        override fun canInstallPackages(): Boolean = systemInstaller.canInstallPackages()

        override fun install(app: TrackedApp) {
            if (state.value[app.id]?.isActive == true) return
            set(app.id, InstallState.Downloading(bytesRead = 0, totalBytes = null))
            jobs[app.id] =
                scope.launch {
                    try {
                        val result = run(app)
                        set(app.id, result)
                    } finally {
                        jobs.remove(app.id)
                    }
                }
        }

        override fun cancel(appId: Long) {
            jobs.remove(appId)?.cancel()
            state.update { it - appId }
        }

        override fun onConfirmationShown(appId: Long) {
            state.update { installs ->
                when (installs[appId]) {
                    is InstallState.WaitingForUser -> installs + (appId to InstallState.Installing)
                    else -> installs
                }
            }
        }

        override fun dismiss(appId: Long) {
            state.update { installs -> if (installs[appId]?.isActive == true) installs else installs - appId }
        }

        /** Installs [app] and returns the final state. */
        private suspend fun run(app: TrackedApp): InstallState {
            val asset =
                app.latestRelease?.let { release ->
                    ApkAssets.select(release.assets, deviceInfo.supportedAbis, app.preferredAssetName)
                }
            return when {
                !canInstallPackages() -> {
                    failed(InstallFailure.PermissionMissing)
                }

                asset == null -> {
                    failed(InstallFailure.NoCompatibleApk)
                }

                else -> {
                    downloadAndInstall(app, asset)
                }
            }
        }

        private suspend fun downloadAndInstall(
            app: TrackedApp,
            asset: ReleaseAsset,
        ): InstallState {
            val download =
                downloader.download(app.repository, asset) { bytesRead, totalBytes ->
                    set(app.id, InstallState.Downloading(bytesRead, totalBytes))
                }
            return when (download) {
                is DownloadResult.Failure -> {
                    failed(download.failure)
                }

                is DownloadResult.Success -> {
                    try {
                        verifyAndInstall(app, asset, download)
                    } finally {
                        download.file.delete()
                    }
                }
            }
        }

        private suspend fun verifyAndInstall(
            app: TrackedApp,
            asset: ReleaseAsset,
            download: DownloadResult.Success,
        ): InstallState {
            val file = download.file
            set(app.id, InstallState.Verifying)
            val apk = inspector.inspectApk(file)
            val failure =
                ApkVerifier.verify(
                    file = DownloadedFile(download.size, download.sha256, asset.size.takeIf { it > 0 }, asset.sha256),
                    apk = apk,
                    expectedPackageName = app.packageName,
                    installed = apk?.let { inspector.installedPackage(it.packageName) },
                )
            return if (failure != null || apk == null) {
                failed(failure ?: InstallFailure.InvalidApk)
            } else {
                set(app.id, InstallState.Installing)
                installVerified(app, apk, file)
            }
        }

        private suspend fun installVerified(
            app: TrackedApp,
            apk: ApkInfo,
            file: File,
        ): InstallState {
            var finalState: InstallState = failed(InstallFailure.SystemError(null))
            systemInstaller.install(file, apk.packageName, app.displayName).collect { result ->
                when (result) {
                    is SessionResult.PendingUserAction -> {
                        set(app.id, InstallState.WaitingForUser(result.intent))
                    }

                    SessionResult.Success -> {
                        if (app.packageName == null) trackedAppsRepository.setPackageName(app.id, apk.packageName)
                        finalState = InstallState.Succeeded(apk.versionName)
                    }

                    is SessionResult.Failure -> {
                        finalState = failed(result.failure)
                    }
                }
            }
            return finalState
        }

        private fun failed(failure: InstallFailure) = InstallState.Failed(failure)

        private fun set(
            appId: Long,
            installState: InstallState,
        ) {
            state.update { it + (appId to installState) }
        }
    }
