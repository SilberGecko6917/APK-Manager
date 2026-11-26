package me.geckotv.apkmanager.core.installer

import android.app.Notification
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.installer.internal.InstallNotifier
import me.geckotv.apkmanager.core.installer.internal.InstallServiceController
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.testing.FakeAppInstaller
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InstallNotificationsTest {
    private val installer = FakeAppInstaller()
    private val repository =
        FakeTrackedAppsRepository(
            listOf(
                TestData.trackedApp(id = 1, repository = GitHubRepository("octo", "notes")),
                TestData.trackedApp(id = 2, repository = GitHubRepository("octo", "player")),
            ),
        )
    private val notifier = RecordingNotifier()
    private val service = RecordingServiceController()

    private fun TestScope.start() {
        InstallNotifications(
            installer,
            repository,
            notifier,
            service,
            CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler)),
        ).start()
        runCurrent()
    }

    @Test
    fun runningInstalls_areShownAndKeepTheServiceRunning() =
        runTest {
            start()

            installer.setState(1, InstallState.Downloading(bytesRead = 40, totalBytes = 100))
            installer.setState(2, InstallState.WaitingForUser(Intent("confirm")))
            advanceTimeBy(UPDATE_INTERVAL)

            assertEquals(setOf(1L, 2L), notifier.shown.keys)
            assertEquals("notes", notifier.shown.getValue(1).first)
            assertEquals(2, service.runningCount)
        }

    @Test
    fun finishedInstalls_areRemovedAndStopTheService() =
        runTest {
            start()
            installer.setState(1, InstallState.Downloading(bytesRead = 40, totalBytes = 100))
            advanceTimeBy(UPDATE_INTERVAL)

            installer.setState(1, InstallState.Succeeded("1.0"))
            advanceTimeBy(UPDATE_INTERVAL)

            assertTrue(notifier.shown.isEmpty())
            assertEquals(listOf(1L), notifier.cancelled)
            assertEquals(0, service.runningCount)
        }

    @Test
    fun progressUpdates_areThrottled() =
        runTest {
            start()

            repeat(10) { installer.setState(1, InstallState.Downloading(bytesRead = it.toLong(), totalBytes = 100)) }
            advanceTimeBy(UPDATE_INTERVAL)

            assertEquals(1, notifier.showCount)
            assertEquals(InstallState.Downloading(9, 100), notifier.shown.getValue(1).second)
        }

    private class RecordingNotifier : InstallNotifier {
        val shown = mutableMapOf<Long, Pair<String, InstallState>>()
        val cancelled = mutableListOf<Long>()
        var showCount = 0

        override fun show(
            appId: Long,
            name: String,
            state: InstallState,
        ) {
            showCount++
            shown[appId] = name to state
        }

        override fun cancel(appId: Long) {
            cancelled += appId
            shown -= appId
        }

        override fun summary(count: Int): Notification = throw UnsupportedOperationException()
    }

    private class RecordingServiceController : InstallServiceController {
        var runningCount = 0

        override fun start(count: Int) {
            runningCount = count
        }

        override fun stop() {
            runningCount = 0
        }
    }

    private companion object {
        const val UPDATE_INTERVAL = 600L
    }
}
