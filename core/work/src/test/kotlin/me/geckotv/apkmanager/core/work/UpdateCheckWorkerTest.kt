package me.geckotv.apkmanager.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.work.internal.UpdateCheckWorker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateCheckWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = FakeTrackedAppsRepository()
    private val notifier = RecordingNotifier()

    @Test
    fun checksAndNotifies() =
        runTest {
            repository.updateCheckResult = UpdateCheckResult(checkedCount = 2, failures = emptyMap())

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(1, repository.updateCheckCount)
            assertEquals(1, notifier.calls)
        }

    @Test
    fun nothingChecked_retries() =
        runTest {
            repository.updateCheckResult =
                UpdateCheckResult(checkedCount = 0, failures = mapOf(1L to SourceError.Offline))

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            assertEquals(0, notifier.calls)
        }

    @Test
    fun partlyChecked_succeeds() =
        runTest {
            repository.updateCheckResult =
                UpdateCheckResult(checkedCount = 1, failures = mapOf(2L to SourceError.NotFound))

            assertEquals(ListenableWorker.Result.success(), worker().doWork())
        }

    private fun worker(): UpdateCheckWorker =
        TestListenableWorkerBuilder<UpdateCheckWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = UpdateCheckWorker(appContext, workerParameters, repository, notifier)
                },
            ).build()

    private class RecordingNotifier : UpdateNotifier {
        var calls = 0

        override suspend fun notifyUpdates(apps: List<TrackedApp>) {
            calls++
        }
    }
}
