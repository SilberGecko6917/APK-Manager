package me.geckotv.apkmanager.core.work.internal

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.work.UpdateNotifier

/**
 * Checks all tracked apps for new releases and notifies about updates. Retries later (with
 * WorkManager's backoff) when no app could be checked, e.g. without a working connection.
 */
@HiltWorker
class UpdateCheckWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val trackedAppsRepository: TrackedAppsRepository,
        private val notifier: UpdateNotifier,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            val result = trackedAppsRepository.checkForUpdates()
            if (result.checkedCount == 0 && result.failures.isNotEmpty() && runAttemptCount < MAX_RETRIES) {
                return Result.retry()
            }
            notifier.notifyUpdates(trackedAppsRepository.observeTrackedApps().first())
            return Result.success()
        }

        private companion object {
            const val MAX_RETRIES = 3
        }
    }
