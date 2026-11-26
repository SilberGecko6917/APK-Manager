package me.geckotv.apkmanager.core.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.work.internal.AutoUpdateWorker
import javax.inject.Inject

/**
 * Schedules installing automatic updates for when the device meets the constraints in the settings
 * (unmetered network, charging) and the battery is not low.
 */
class AutoUpdateScheduler
    @Inject
    constructor(
        private val workManager: Lazy<WorkManager>,
    ) {
        /** Schedules the automatic updates, replacing a schedule that waits with older constraints. */
        fun schedule(settings: UserSettings) {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(
                        if (settings.autoUpdateOnlyOnUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    ).setRequiresCharging(settings.autoUpdateOnlyWhileCharging)
                    .setRequiresBatteryNotLow(true)
                    .build()
            val request = OneTimeWorkRequestBuilder<AutoUpdateWorker>().setConstraints(constraints).build()
            workManager.get().enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        companion object {
            /** Name of the unique work. */
            const val WORK_NAME = "auto-update"
        }
    }
