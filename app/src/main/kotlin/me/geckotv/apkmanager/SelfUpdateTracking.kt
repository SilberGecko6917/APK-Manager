package me.geckotv.apkmanager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.model.GitHubRepositoryParser
import javax.inject.Inject

/**
 * Adds APK Manager's own repository to the tracked apps once, so APK Manager can update itself like
 * any other app. If the user removes it, it stays removed.
 */
class SelfUpdateTracking
    @Inject
    constructor(
        private val trackedAppsRepository: TrackedAppsRepository,
        private val userSettingsRepository: UserSettingsRepository,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        /**
         * @param repository `owner/name` of APK Manager's repository.
         * @param packageName APK Manager's package name.
         * @param versionName the running version; pre-release versions track pre-releases.
         */
        fun start(
            repository: String,
            packageName: String,
            versionName: String,
        ) {
            val gitHubRepository = GitHubRepositoryParser.parse(repository) ?: return
            scope.launch {
                if (userSettingsRepository.settings.first().selfUpdatesTracked) return@launch
                trackedAppsRepository.trackRepository(
                    repository = gitHubRepository,
                    packageName = packageName,
                    displayName = APP_NAME,
                    includePrereleases = versionName.contains('-'),
                )
                userSettingsRepository.setSelfUpdatesTracked(true)
            }
        }

        private companion object {
            const val APP_NAME = "APK Manager"
        }
    }
