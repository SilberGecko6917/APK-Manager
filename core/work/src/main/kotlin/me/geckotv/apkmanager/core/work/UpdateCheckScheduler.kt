package me.geckotv.apkmanager.core.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.work.internal.UpdateCheckWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the periodic background update check in line with the settings: scheduled with the chosen
 * interval and network constraint while background checks are on, cancelled while they are off.
 *
 * [workManager] is [Lazy] on purpose: the application injects this class, and getting WorkManager
 * initializes it with the application's configuration, which needs the application's own injected
 * worker factory. Getting it during injection crashed the app at start (0.1.0-alpha.7).
 */
@Singleton
class UpdateCheckScheduler
    @Inject
    constructor(
        private val workManager: Lazy<WorkManager>,
        private val userSettingsRepository: UserSettingsRepository,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        /** Starts following the settings. Call once, when the app starts. */
        fun start() {
            scope.launch {
                userSettingsRepository.settings
                    .map {
                        ScheduleSettings(
                            it.backgroundChecksEnabled,
                            it.checkInterval,
                            it.checkOnlyOnUnmeteredNetwork,
                        )
                    }.distinctUntilChanged()
                    .collect(::apply)
            }
        }

        private fun apply(settings: ScheduleSettings) {
            if (!settings.enabled) {
                workManager.get().cancelUniqueWork(WORK_NAME)
                return
            }
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(
                        if (settings.onlyUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    ).setRequiresBatteryNotLow(true)
                    .build()
            val request =
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(settings.interval.hours, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
            workManager.get().enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        private data class ScheduleSettings(
            val enabled: Boolean,
            val interval: UpdateCheckInterval,
            val onlyUnmetered: Boolean,
        )

        companion object {
            /** Name of the unique periodic work. */
            const val WORK_NAME = "update-check"
        }
    }
