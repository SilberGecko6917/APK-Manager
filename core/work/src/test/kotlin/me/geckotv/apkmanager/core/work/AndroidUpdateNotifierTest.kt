package me.geckotv.apkmanager.core.work

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.TestData
import me.geckotv.apkmanager.core.work.internal.AndroidUpdateNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AndroidUpdateNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private val update =
        TestData.trackedApp(
            id = 1,
            repository = GitHubRepository("octo", "notes"),
            latestRelease = TestData.release(id = 11, tag = "v2.0.0"),
            installedApp = TestData.installedApp("com.example.notes", "Notes", "1.0.0"),
        )
    private val upToDate =
        TestData.trackedApp(
            id = 2,
            repository = GitHubRepository("octo", "player"),
            latestRelease = TestData.release(id = 21, tag = "v1.0.0"),
            installedApp = TestData.installedApp("com.example.player", "Player", "1.0.0"),
        )

    @Test
    fun update_isNotifiedOnce() =
        runTest {
            val repository = FakeTrackedAppsRepository(listOf(update, upToDate))
            val notifier = AndroidUpdateNotifier(context, repository)

            notifier.notifyUpdates(repository.trackedApps)

            val notifications = shadowOf(notificationManager).allNotifications
            assertEquals(2, notifications.size) // the update and the group summary
            assertTrue(notifications.any { shadowOf(it).contentTitle == "Update for Notes" })
            assertEquals(11L, repository.trackedApps.first { it.id == 1L }.notifiedReleaseId)

            notificationManager.cancelAll()
            notifier.notifyUpdates(repository.trackedApps)

            // Only the summary is posted again; the update itself was notified already.
            assertEquals(1, shadowOf(notificationManager).allNotifications.size)
        }

    @Test
    fun newRelease_isNotifiedAgain() =
        runTest {
            val notified: TrackedApp = update.copy(notifiedReleaseId = 11)
            val newer = notified.copy(latestRelease = TestData.release(id = 12, tag = "v2.1.0"))
            val repository = FakeTrackedAppsRepository(listOf(newer))

            AndroidUpdateNotifier(context, repository).notifyUpdates(repository.trackedApps)

            assertTrue(
                shadowOf(notificationManager).allNotifications.any { shadowOf(it).contentText == "1.0.0 → v2.1.0" },
            )
            assertEquals(12L, repository.trackedApps.single().notifiedReleaseId)
        }

    @Test
    fun noUpdates_removesTheSummary() =
        runTest {
            val repository = FakeTrackedAppsRepository(listOf(update))
            val notifier = AndroidUpdateNotifier(context, repository)
            notifier.notifyUpdates(repository.trackedApps)

            notifier.notifyUpdates(listOf(update.copy(latestRelease = TestData.release(id = 11, tag = "v1.0.0"))))

            assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
        }
}
