package me.geckotv.apkmanager.core.work

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UpdateStatus
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeAppInstaller
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoUpdaterTest {
    private val notifier = RecordingNotifier()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var workManager: WorkManager
    private val scheduler = AutoUpdateScheduler { workManager }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun scheduleUpdates_schedulesOnlyWithCandidates() =
        runTest {
            assertTrue(updater(listOf(app(id = 1, policy = null))).scheduleUpdates(OWN_PACKAGE).isEmpty())
            assertTrue(workManager.getWorkInfosForUniqueWork(AutoUpdateScheduler.WORK_NAME).get().isEmpty())

            val scheduled = updater(listOf(app(id = 1, policy = UpdatePolicy.Install))).scheduleUpdates(OWN_PACKAGE)

            assertEquals(listOf(1L), scheduled.map { it.id })
            assertEquals(1, workManager.getWorkInfosForUniqueWork(AutoUpdateScheduler.WORK_NAME).get().size)
        }

    @Test
    fun candidates_followPolicyAndUpdateStatus() =
        runTest {
            val apps =
                listOf(
                    app(id = 1, policy = UpdatePolicy.Install),
                    app(id = 2, policy = null),
                    app(id = 3, policy = UpdatePolicy.Notify),
                    app(id = 4, policy = UpdatePolicy.Install, installedVersion = "2.0.0"),
                    app(id = 5, policy = UpdatePolicy.Install, installed = false),
                    app(id = 6, policy = UpdatePolicy.Install).copy(autoUpdateReleaseId = 106),
                )

            val defaultNotify = updater(apps).candidates(OWN_PACKAGE).map { it.id }
            val defaultInstall =
                updater(
                    apps,
                    UserSettings(defaultUpdatePolicy = UpdatePolicy.Install),
                ).candidates(OWN_PACKAGE).map { it.id }

            assertEquals(listOf(1L), defaultNotify)
            assertEquals(listOf(1L, 2L), defaultInstall)
        }

    @Test
    fun candidates_apkManagerComesLast() =
        runTest {
            val apps =
                listOf(
                    app(id = 1, policy = UpdatePolicy.Install, packageName = OWN_PACKAGE),
                    app(id = 2, policy = UpdatePolicy.Install),
                )

            assertEquals(listOf(2L, 1L), updater(apps).candidates(OWN_PACKAGE).map { it.id })
        }

    @Test
    fun success_isRememberedAndNotified() =
        runTest {
            val repository = FakeTrackedAppsRepository(listOf(app(id = 1, policy = UpdatePolicy.Install)))
            val installer = FakeAppInstaller(stateAfterInstall = InstallState.Succeeded("2.0.0"))

            val result =
                AutoUpdater(
                    repository,
                    FakeUserSettingsRepository(),
                    installer,
                    notifier,
                    scheduler,
                ).updateAll(OWN_PACKAGE)

            assertEquals(AutoUpdateResult(updatedCount = 1, retryLater = false), result)
            assertEquals(listOf(AutoUpdate(1, "App 1", "2.0.0")), notifier.autoUpdates)
            assertEquals(101L, repository.trackedApps.single().autoUpdateReleaseId)
            // The finished install is dismissed, so the app list does not show it.
            assertNull(installer.installs.value[1L])
        }

    @Test
    fun permanentFailure_isNotTriedAgainAndNotifiedAsUpdate() =
        runTest {
            val repository = FakeTrackedAppsRepository(listOf(app(id = 1, policy = UpdatePolicy.Install)))
            val installer = FakeAppInstaller(stateAfterInstall = InstallState.Failed(InstallFailure.SignatureMismatch))
            val updater = AutoUpdater(repository, FakeUserSettingsRepository(), installer, notifier, scheduler)

            val result = updater.updateAll(OWN_PACKAGE)

            assertFalse(result.retryLater)
            assertEquals(101L, repository.trackedApps.single().autoUpdateReleaseId)
            assertEquals(listOf(1L), notifier.notifiedAppIds)
            assertTrue(updater.candidates(OWN_PACKAGE).isEmpty())
        }

    @Test
    fun passingFailure_isTriedAgainLater() =
        runTest {
            val repository = FakeTrackedAppsRepository(listOf(app(id = 1, policy = UpdatePolicy.Install)))
            val installer =
                FakeAppInstaller(
                    stateAfterInstall = InstallState.Failed(InstallFailure.DownloadFailed(SourceError.Offline)),
                )
            val updater = AutoUpdater(repository, FakeUserSettingsRepository(), installer, notifier, scheduler)

            val result = updater.updateAll(OWN_PACKAGE)

            assertTrue(result.retryLater)
            assertNull(repository.trackedApps.single().autoUpdateReleaseId)
            assertEquals(emptyList<Long>(), notifier.notifiedAppIds)
            assertEquals(listOf(1L), updater.candidates(OWN_PACKAGE).map { it.id })
        }

    @Test
    fun confirmationNeeded_isLeftToTheInstallNotification() =
        runTest {
            val repository = FakeTrackedAppsRepository(listOf(app(id = 1, policy = UpdatePolicy.Install)))
            val installer = FakeAppInstaller(stateAfterInstall = InstallState.WaitingForUser(Intent()))

            val result =
                AutoUpdater(
                    repository,
                    FakeUserSettingsRepository(),
                    installer,
                    notifier,
                    scheduler,
                ).updateAll(OWN_PACKAGE)

            assertEquals(AutoUpdateResult(updatedCount = 0, retryLater = false), result)
            assertEquals(101L, repository.trackedApps.single().autoUpdateReleaseId)
            assertEquals(emptyList<Long>(), notifier.notifiedAppIds)
        }

    private fun updater(
        apps: List<TrackedApp>,
        settings: UserSettings = UserSettings(),
    ) = AutoUpdater(
        FakeTrackedAppsRepository(apps),
        FakeUserSettingsRepository(settings),
        FakeAppInstaller(),
        notifier,
        scheduler,
    )

    private fun app(
        id: Long,
        policy: UpdatePolicy?,
        installed: Boolean = true,
        installedVersion: String = "1.0.0",
        packageName: String = "com.example.app$id",
    ): TrackedApp =
        TestData
            .trackedApp(
                id = id,
                repository = GitHubRepository("octo", "app$id"),
                latestRelease = TestData.release(id = 100 + id, tag = "v2.0.0"),
                installedApp = if (installed) TestData.installedApp(packageName, "App $id", installedVersion) else null,
                packageName = packageName,
            ).copy(updatePolicy = policy)

    private class RecordingNotifier : UpdateNotifier {
        val autoUpdates = mutableListOf<AutoUpdate>()
        val notifiedAppIds = mutableListOf<Long>()

        override suspend fun notifyUpdates(apps: List<TrackedApp>) {
            notifiedAppIds += apps.filter { it.updateStatus == UpdateStatus.UpdateAvailable }.map { it.id }
        }

        override suspend fun notifyAutoUpdates(updates: List<AutoUpdate>) {
            autoUpdates += updates
        }
    }

    private companion object {
        const val OWN_PACKAGE = "me.geckotv.apkmanager"
    }
}
