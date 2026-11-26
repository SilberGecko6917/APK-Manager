package me.geckotv.apkmanager.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import me.geckotv.apkmanager.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoUpdateSchedulerTest {
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
    fun defaults_waitForUnmeteredNetwork() {
        AutoUpdateScheduler { workManager }.schedule(UserSettings())

        val work = scheduledWork().single()
        assertEquals(NetworkType.UNMETERED, work.constraints.requiredNetworkType)
        assertFalse(work.constraints.requiresCharging())
        assertTrue(work.constraints.requiresBatteryNotLow())
    }

    @Test
    fun settings_setTheConstraints_andReplaceTheSchedule() {
        val scheduler = AutoUpdateScheduler { workManager }
        scheduler.schedule(UserSettings())

        scheduler.schedule(UserSettings(autoUpdateOnlyOnUnmeteredNetwork = false, autoUpdateOnlyWhileCharging = true))

        val work = scheduledWork().single { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
        assertTrue(work.constraints.requiresCharging())
    }

    private fun scheduledWork(): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(AutoUpdateScheduler.WORK_NAME).get()
}
