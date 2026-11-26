package me.geckotv.apkmanager.core.work.internal

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.geckotv.apkmanager.core.work.AutoUpdater

/** Installs automatic updates; retries with WorkManager's backoff if some failed for a passing reason. */
@HiltWorker
class AutoUpdateWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val autoUpdater: AutoUpdater,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            val result = autoUpdater.updateAll(ownPackageName = applicationContext.packageName)
            return if (result.retryLater && runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        }

        private companion object {
            const val MAX_RETRIES = 3
        }
    }
