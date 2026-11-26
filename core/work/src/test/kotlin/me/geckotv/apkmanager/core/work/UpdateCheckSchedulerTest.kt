package me.geckotv.apkmanager.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class UpdateCheckSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun enabled_schedulesPeriodicCheckWithSettings() =
        runTest {
            val settings =
                FakeUserSettingsRepository(
                    UserSettings(checkInterval = UpdateCheckInterval.Every6Hours, checkOnlyOnUnmeteredNetwork = true),
                )

            UpdateCheckScheduler(
                { workManager },
                settings,
                CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            ).start()

            val work = scheduledWork().single()
            assertEquals(WorkInfo.State.ENQUEUED, work.state)
            assertEquals(TimeUnit.HOURS.toMillis(6), work.periodicityInfo?.repeatIntervalMillis)
            assertEquals(NetworkType.UNMETERED, work.constraints.requiredNetworkType)
            assertTrue(work.constraints.requiresBatteryNotLow())
        }

    @Test
    fun changedSettings_updateTheSchedule() =
        runTest {
            val settings = FakeUserSettingsRepository()
            UpdateCheckScheduler(
                { workManager },
                settings,
                CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            ).start()

            settings.setCheckInterval(UpdateCheckInterval.Every24Hours)

            val work = scheduledWork().single { it.state == WorkInfo.State.ENQUEUED }
            assertEquals(TimeUnit.HOURS.toMillis(24), work.periodicityInfo?.repeatIntervalMillis)
            assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
        }

    @Test
    fun disabled_cancelsTheCheck() =
        runTest {
            val settings = FakeUserSettingsRepository()
            UpdateCheckScheduler(
                { workManager },
                settings,
                CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            ).start()

            settings.setBackgroundChecksEnabled(false)

            assertTrue(scheduledWork().all { it.state == WorkInfo.State.CANCELLED })
        }

    private fun scheduledWork(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(UpdateCheckScheduler.WORK_NAME).get()
}
