package me.geckotv.apkmanager.core.work

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UpdateStatus
import javax.inject.Inject

/**
 * Installs the updates of apps whose update policy is [UpdatePolicy.Install], one after another, and
 * notifies about the result.
 *
 * Each release is tried once: after it was installed, the user was asked to confirm it, or it failed
 * for good (e.g. a different signing key), APK Manager does not try it again, and failed updates are
 * notified like any other update. Updates that failed for a passing reason (no connection) are tried
 * again later.
 */
class AutoUpdater
    @Inject
    constructor(
        private val trackedAppsRepository: TrackedAppsRepository,
        private val userSettingsRepository: UserSettingsRepository,
        private val appInstaller: AppInstaller,
        private val notifier: UpdateNotifier,
        private val scheduler: AutoUpdateScheduler,
    ) {
        /**
         * The apps with an update that will be installed automatically. [ownPackageName] (APK Manager
         * itself) comes last, because installing it ends the running process.
         */
        suspend fun candidates(ownPackageName: String): List<TrackedApp> {
            val defaultPolicy = userSettingsRepository.settings.first().defaultUpdatePolicy
            return trackedAppsRepository
                .observeTrackedApps()
                .first()
                .filter { it.isAutoUpdateCandidate(defaultPolicy) }
                .sortedBy { it.packageName == ownPackageName }
        }

        /**
         * Schedules installing the [candidates] if there are any (see [AutoUpdateScheduler]) and
         * returns them.
         */
        suspend fun scheduleUpdates(ownPackageName: String): List<TrackedApp> {
            val candidates = candidates(ownPackageName)
            if (candidates.isNotEmpty()) scheduler.schedule(userSettingsRepository.settings.first())
            return candidates
        }

        /** Installs the [candidates] and returns whether some should be tried again later. */
        suspend fun updateAll(ownPackageName: String): AutoUpdateResult {
            val outcomes = candidates(ownPackageName).associateWith { install(it) }
            val retryLater = outcomes.filterValues { it == null || it.isPassingFailure() }.keys
            val done = outcomes - retryLater
            done.keys.forEach { app ->
                app.latestRelease?.let { trackedAppsRepository.setAutoUpdateRelease(app.id, it.id) }
            }
            val updated =
                done.mapNotNull { (app, outcome) ->
                    (outcome as? InstallState.Succeeded)?.let {
                        AutoUpdate(app.id, app.displayName, it.versionName ?: app.latestRelease?.tagName.orEmpty())
                    }
                }
            updated.forEach { appInstaller.dismiss(it.appId) }
            // Apps waiting for the user have an install notification that asks to confirm.
            val waiting = done.filterValues { it is InstallState.WaitingForUser }.keys
            val notNotified = (retryLater + waiting).map { it.id }.toSet()
            notifier.notifyAutoUpdates(updated)
            notifier.notifyUpdates(trackedAppsRepository.observeTrackedApps().first().filter { it.id !in notNotified })
            return AutoUpdateResult(updatedCount = updated.size, retryLater = retryLater.isNotEmpty())
        }

        /**
         * Starts installing [app] and waits until it is done or waits for the user. Returns `null` if
         * it takes too long; the install goes on anyway.
         */
        private suspend fun install(app: TrackedApp): InstallState? {
            appInstaller.install(app)
            return withTimeoutOrNull(INSTALL_TIMEOUT_MILLIS) {
                appInstaller.installs
                    .map { installs -> installs[app.id] ?: InstallState.Failed(InstallFailure.Cancelled) }
                    .first { !it.isActive || it is InstallState.WaitingForUser }
            }
        }

        private companion object {
            /** WorkManager stops workers after ten minutes. */
            const val INSTALL_TIMEOUT_MILLIS = 4 * 60 * 1_000L
        }
    }

/** An app that was updated automatically to [versionName]. */
data class AutoUpdate(
    val appId: Long,
    val appName: String,
    val versionName: String,
)

/** What [AutoUpdater.updateAll] did. */
data class AutoUpdateResult(
    val updatedCount: Int,
    /** Whether some updates failed for a passing reason, e.g. no connection, and should be tried again. */
    val retryLater: Boolean,
)

/**
 * Whether a background check installs the update of this app: its policy is [UpdatePolicy.Install],
 * it is installed, an update is available and APK Manager did not try this release before.
 */
internal fun TrackedApp.isAutoUpdateCandidate(defaultPolicy: UpdatePolicy): Boolean {
    val release = latestRelease ?: return false
    val wantsAutoUpdate = effectiveUpdatePolicy(defaultPolicy) == UpdatePolicy.Install && installedApp != null
    return wantsAutoUpdate && updateStatus == UpdateStatus.UpdateAvailable && release.id != autoUpdateReleaseId
}

/** Whether the install failed for a reason that may pass, so it is worth trying again later. */
private fun InstallState.isPassingFailure(): Boolean {
    val failure = (this as? InstallState.Failed)?.failure ?: return false
    return when (failure) {
        is InstallFailure.DownloadFailed -> failure.error.isPassing()
        is InstallFailure.DeveloperVerificationFailed -> failure.networkUnavailable
        InstallFailure.Storage -> true
        else -> false
    }
}

private fun SourceError.isPassing(): Boolean =
    this is SourceError.Offline || this is SourceError.Unavailable || this is SourceError.RateLimited
