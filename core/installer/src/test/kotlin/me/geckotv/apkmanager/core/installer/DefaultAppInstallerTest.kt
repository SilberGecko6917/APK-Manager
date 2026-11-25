package me.geckotv.apkmanager.core.installer

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.installer.internal.ApkInfo
import me.geckotv.apkmanager.core.installer.internal.AssetDownloader
import me.geckotv.apkmanager.core.installer.internal.DefaultAppInstaller
import me.geckotv.apkmanager.core.installer.internal.DownloadResult
import me.geckotv.apkmanager.core.installer.internal.InstalledPackage
import me.geckotv.apkmanager.core.installer.internal.PackageInspector
import me.geckotv.apkmanager.core.installer.internal.SessionResult
import me.geckotv.apkmanager.core.installer.internal.SystemInstaller
import me.geckotv.apkmanager.core.installer.internal.sha256
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.testing.FakeDeviceInfo
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DefaultAppInstallerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val content = "apk content".toByteArray()
    private val asset = TestData.asset(id = 10, name = "app-arm64-v8a.apk", size = content.size.toLong())
    private val app =
        TestData.trackedApp(
            id = 1,
            repository = GitHubRepository("octo", "app"),
            latestRelease = TestData.release(id = 1, tag = "v1.1.0", assets = listOf(asset)),
            installedApp = TestData.installedApp("com.example.app", "Example", "1.0.0"),
        )

    private val downloader = FakeDownloader()
    private val inspector = FakeInspector()
    private val systemInstaller = FakeSystemInstaller()
    private val repository = FakeTrackedAppsRepository(listOf(app))

    private fun TestScope.installer() =
        DefaultAppInstaller(
            downloader = downloader,
            inspector = inspector,
            systemInstaller = systemInstaller,
            trackedAppsRepository = repository,
            deviceInfo = FakeDeviceInfo(),
            scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
        )

    @Test
    fun install_downloadsVerifiesAndInstalls() =
        runTest {
            val installer = installer()

            installer.install(app)

            assertEquals(InstallState.Succeeded("1.1.0"), installer.installs.value[1])
            assertEquals(listOf(asset.id), downloader.downloadedAssetIds)
            assertEquals("com.example.app", systemInstaller.installedPackageName)
            assertFalse("The download is deleted", downloader.lastFile!!.exists())
        }

    @Test
    fun install_withoutPermission_fails() =
        runTest {
            systemInstaller.canInstall = false
            val installer = installer()

            installer.install(app)

            assertEquals(InstallState.Failed(InstallFailure.PermissionMissing), installer.installs.value[1])
            assertTrue(downloader.downloadedAssetIds.isEmpty())
        }

    @Test
    fun install_withoutCompatibleApk_fails() =
        runTest {
            val x86Only =
                app.copy(
                    latestRelease =
                        TestData.release(
                            1,
                            "v1.1.0",
                            assets = listOf(TestData.asset(11, "app-x86_64.apk")),
                        ),
                )
            val installer = installer()

            installer.install(x86Only)

            assertEquals(InstallState.Failed(InstallFailure.NoCompatibleApk), installer.installs.value[1])
        }

    @Test
    fun install_downloadFailure_fails() =
        runTest {
            downloader.failure = InstallFailure.DownloadFailed(SourceError.Offline)
            val installer = installer()

            installer.install(app)

            assertEquals(
                InstallState.Failed(InstallFailure.DownloadFailed(SourceError.Offline)),
                installer.installs.value[1],
            )
            assertNull(systemInstaller.installedPackageName)
        }

    @Test
    fun install_otherSigner_isNotInstalled() =
        runTest {
            inspector.installed = InstalledPackage(versionCode = 1, signers = setOf("other-key"))
            val installer = installer()

            installer.install(app)

            assertEquals(InstallState.Failed(InstallFailure.SignatureMismatch), installer.installs.value[1])
            assertNull(systemInstaller.installedPackageName)
        }

    @Test
    fun install_otherPackage_isNotInstalled() =
        runTest {
            inspector.apk = inspector.apk.copy(packageName = "com.evil.app")
            val installer = installer()

            installer.install(app)

            assertEquals(
                InstallState.Failed(InstallFailure.PackageMismatch("com.example.app", "com.evil.app")),
                installer.installs.value[1],
            )
        }

    @Test
    fun install_notTrackedPackage_linksInstalledPackage() =
        runTest {
            val notInstalled = app.copy(packageName = null, installedApp = null)
            inspector.installed = null
            val installer = installer()

            installer.install(notInstalled)

            assertEquals(InstallState.Succeeded("1.1.0"), installer.installs.value[1])
            assertEquals("com.example.app", repository.trackedApps.single().packageName)
        }

    @Test
    fun install_waitsForUserConfirmation() =
        runTest {
            val results = MutableSharedFlow<SessionResult>()
            systemInstaller.results = results.takeWhile { it !is SessionResult.Success }
            val installer = installer()
            val confirmation = Intent("confirm")

            installer.install(app)
            results.emit(SessionResult.PendingUserAction(confirmation))

            assertEquals(InstallState.WaitingForUser(confirmation), installer.installs.value[1])
            installer.onConfirmationShown(1)
            assertEquals(InstallState.Installing, installer.installs.value[1])
        }

    @Test
    fun install_systemFailure_isReported() =
        runTest {
            systemInstaller.results = flowOf(SessionResult.Failure(InstallFailure.Cancelled))
            val installer = installer()

            installer.install(app)

            assertEquals(InstallState.Failed(InstallFailure.Cancelled), installer.installs.value[1])
        }

    @Test
    fun cancel_stopsAndForgetsInstall() =
        runTest {
            systemInstaller.results = MutableSharedFlow()
            val installer = installer()
            installer.install(app)

            installer.cancel(1)

            assertNull(installer.installs.value[1])
        }

    @Test
    fun dismiss_forgetsFinishedInstallOnly() =
        runTest {
            val installer = installer()
            installer.install(app)

            installer.dismiss(1)

            assertNull(installer.installs.value[1])
        }

    private inner class FakeDownloader : AssetDownloader {
        var failure: InstallFailure? = null
        val downloadedAssetIds = mutableListOf<Long>()
        var lastFile: File? = null

        override suspend fun download(
            repository: GitHubRepository,
            asset: ReleaseAsset,
            onProgress: (Long, Long?) -> Unit,
        ): DownloadResult {
            downloadedAssetIds += asset.id
            val target = File(context.cacheDir, "downloads/${asset.id}.apk")
            lastFile = target
            failure?.let { return DownloadResult.Failure(it) }
            target.parentFile?.mkdirs()
            target.writeBytes(content)
            onProgress(content.size.toLong(), content.size.toLong())
            return DownloadResult.Success(file = target, size = content.size.toLong(), sha256 = content.sha256())
        }
    }

    private class FakeInspector : PackageInspector {
        var apk =
            ApkInfo(
                packageName = "com.example.app",
                versionCode = 2,
                versionName = "1.1.0",
                signers = setOf("key"),
                signingHistory = setOf("key"),
            )
        var installed: InstalledPackage? = InstalledPackage(versionCode = 1, signers = setOf("key"))

        override fun inspectApk(file: File): ApkInfo = apk

        override fun installedPackage(packageName: String): InstalledPackage? = installed
    }

    private class FakeSystemInstaller : SystemInstaller {
        var results: Flow<SessionResult> = flowOf(SessionResult.Success)
        var installedPackageName: String? = null
        var canInstall = true

        override fun canInstallPackages(): Boolean = canInstall

        override fun install(
            file: File,
            packageName: String,
            label: String,
        ): Flow<SessionResult> {
            installedPackageName = packageName
            return results
        }
    }
}
