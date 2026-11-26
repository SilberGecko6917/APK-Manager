package me.geckotv.apkmanager.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.repository.UpdateCheckResult
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.testing.FakeAppInstaller
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import me.geckotv.apkmanager.core.testing.TestData
import me.geckotv.apkmanager.core.work.internal.UpdateCheckWorker
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateCheckWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notifier = RecordingNotifier()
    private val settings = FakeUserSettingsRepository()
    private var repository = FakeTrackedAppsRepository()
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

    @Test
    fun automaticUpdates_areScheduledInsteadOfNotified() =
        runTest {
            val autoUpdate = app(id = 1, policy = UpdatePolicy.Install)
            val notifyOnly = app(id = 2, policy = null)
            repository = FakeTrackedAppsRepository(listOf(autoUpdate, notifyOnly))
            repository.updateCheckResult = UpdateCheckResult(checkedCount = 2, failures = emptyMap())

            worker().doWork()

            assertEquals(listOf(2L), notifier.appIds)
            val work = workManager.getWorkInfosForUniqueWork(AutoUpdateScheduler.WORK_NAME).get().single()
            assertEquals(WorkInfo.State.ENQUEUED, work.state)
        }

    @Test
    fun noAutomaticUpdates_scheduleNothing() =
        runTest {
            repository = FakeTrackedAppsRepository(listOf(app(id = 1, policy = null)))
            repository.updateCheckResult = UpdateCheckResult(checkedCount = 1, failures = emptyMap())

            worker().doWork()

            assertEquals(
                emptyList<WorkInfo>(),
                workManager.getWorkInfosForUniqueWork(AutoUpdateScheduler.WORK_NAME).get(),
            )
        }

    private fun app(
        id: Long,
        policy: UpdatePolicy?,
    ): TrackedApp =
        TestData
            .trackedApp(
                id = id,
                repository = GitHubRepository("octo", "app$id"),
                latestRelease = TestData.release(id = 100 + id, tag = "v2.0.0"),
                installedApp = TestData.installedApp("com.example.app$id", "App $id", "1.0.0"),
            ).copy(updatePolicy = policy)

    private fun worker(): UpdateCheckWorker =
        TestListenableWorkerBuilder<UpdateCheckWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        UpdateCheckWorker(
                            appContext,
                            workerParameters,
                            repository,
                            notifier,
                            AutoUpdater(
                                repository,
                                settings,
                                FakeAppInstaller(),
                                notifier,
                                AutoUpdateScheduler { workManager },
                            ),
                        )
                },
            ).build()

    private class RecordingNotifier : UpdateNotifier {
        var calls = 0
        var appIds = emptyList<Long>()

        override suspend fun notifyUpdates(apps: List<TrackedApp>) {
            calls++
            appIds = apps.map { it.id }
        }

        override suspend fun notifyAutoUpdates(updates: List<AutoUpdate>) = Unit
    }
}
