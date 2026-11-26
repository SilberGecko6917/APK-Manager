package me.geckotv.apkmanager.core.installer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.installer.internal.RoutingSystemInstaller
import me.geckotv.apkmanager.core.installer.internal.SessionResult
import me.geckotv.apkmanager.core.installer.internal.SystemInstaller
import me.geckotv.apkmanager.core.model.InstallerBackend
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeInstallerBackends
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RoutingSystemInstallerTest {
    private val system = RecordingInstaller(canInstall = false)
    private val shizuku = RecordingInstaller(canInstall = true)
    private val file = File("app.apk")

    @Test
    fun systemSelected_usesThePackageInstaller() =
        runTest {
            val router = router(InstallerBackend.System, ShizukuState.Ready(isRoot = false))

            router.install(file, "com.example.app", "App").toList()

            assertEquals(1, system.installs)
            assertEquals(0, shizuku.installs)
        }

    @Test
    fun shizukuSelectedAndReady_usesShizuku() =
        runTest {
            val router = router(InstallerBackend.Shizuku, ShizukuState.Ready(isRoot = false))

            router.install(file, "com.example.app", "App").toList()

            assertEquals(1, shizuku.installs)
            assertTrue("Shizuku needs no install permission", router.canInstallPackages())
        }

    @Test
    fun shizukuSelectedButNotRunning_fallsBackToThePackageInstaller() =
        runTest {
            val router = router(InstallerBackend.Shizuku, ShizukuState.NotRunning)

            router.install(file, "com.example.app", "App").toList()

            assertEquals(1, system.installs)
            assertEquals(0, shizuku.installs)
            assertFalse(router.canInstallPackages())
        }

    private fun router(
        selected: InstallerBackend,
        shizukuState: ShizukuState,
    ): RoutingSystemInstaller {
        val active =
            if (selected == InstallerBackend.Shizuku &&
                shizukuState is ShizukuState.Ready
            ) {
                selected
            } else {
                InstallerBackend.System
            }
        return RoutingSystemInstaller(
            system = system,
            shizuku = shizuku,
            backends = FakeInstallerBackends(shizukuState, active),
            userSettingsRepository = FakeUserSettingsRepository(UserSettings(installerBackend = selected)),
        )
    }

    private class RecordingInstaller(
        private val canInstall: Boolean,
    ) : SystemInstaller {
        var installs = 0

        override fun canInstallPackages(): Boolean = canInstall

        override fun install(
            file: File,
            packageName: String,
            label: String,
        ): Flow<SessionResult> {
            installs++
            return flowOf(SessionResult.Success)
        }
    }
}
