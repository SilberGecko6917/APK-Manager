package me.geckotv.apkmanager.core.installer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.installer.internal.InstallNotifier
import me.geckotv.apkmanager.core.installer.internal.InstallServiceController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shows running installs as notifications (progress, cancel, "tap to install") and keeps a
 * foreground service running while any install is active, so Android does not stop a download when
 * the user leaves the app.
 */
@Singleton
class InstallNotifications
    @Inject
    internal constructor(
        private val appInstaller: AppInstaller,
        private val trackedAppsRepository: TrackedAppsRepository,
        private val notifier: InstallNotifier,
        private val serviceController: InstallServiceController,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        /** Starts following the installs. Call once, when the app starts. */
        @OptIn(FlowPreview::class)
        fun start() {
            scope.launch {
                combine(appInstaller.installs, trackedAppsRepository.observeTrackedApps()) { installs, apps ->
                    val names = apps.associate { it.id to it.displayName }
                    installs
                        .filterValues { it.isActive }
                        .mapValues { (appId, state) -> RunningInstall(names[appId].orEmpty(), state) }
                }.sample(UPDATE_INTERVAL_MILLIS)
                    .collect(::show)
            }
        }

        private var shownAppIds = emptySet<Long>()

        private fun show(running: Map<Long, RunningInstall>) {
            (shownAppIds - running.keys).forEach(notifier::cancel)
            running.forEach { (appId, install) -> notifier.show(appId, install.name, install.state) }
            shownAppIds = running.keys
            if (running.isEmpty()) serviceController.stop() else serviceController.start(running.size)
        }

        private data class RunningInstall(
            val name: String,
            val state: InstallState,
        )

        private companion object {
            /** Android drops notification updates that come faster than a few per second. */
            const val UPDATE_INTERVAL_MILLIS = 500L
        }
    }
