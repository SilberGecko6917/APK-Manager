package me.geckotv.apkmanager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.UserSettings
import me.geckotv.apkmanager.core.testing.FakeTrackedAppsRepository
import me.geckotv.apkmanager.core.testing.FakeUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfUpdateTrackingTest {
    private val trackedApps = FakeTrackedAppsRepository()

    @Test
    fun firstStart_tracksOwnRepositoryOnce() =
        runTest {
            val settings = FakeUserSettingsRepository()
            val tracking =
                SelfUpdateTracking(trackedApps, settings, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            tracking.start("SilberGecko6917/APK-Manager", "me.geckotv.apkmanager", "0.1.0-alpha.2")

            val app = trackedApps.trackedApps.single()
            assertEquals(GitHubRepository("SilberGecko6917", "APK-Manager"), app.repository)
            assertEquals("me.geckotv.apkmanager", app.packageName)
            assertEquals("APK Manager", app.displayName)
            assertTrue("Pre-release builds follow pre-releases", app.includePrereleases)
            assertTrue(settings.current.selfUpdatesTracked)
        }

    @Test
    fun stableVersion_tracksStableReleasesOnly() =
        runTest {
            val tracking =
                SelfUpdateTracking(
                    trackedApps,
                    FakeUserSettingsRepository(),
                    CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                )

            tracking.start("SilberGecko6917/APK-Manager", "me.geckotv.apkmanager", "1.0.0")

            assertEquals(false, trackedApps.trackedApps.single().includePrereleases)
        }

    @Test
    fun removedByUser_isNotAddedAgain() =
        runTest {
            val settings = FakeUserSettingsRepository(UserSettings(selfUpdatesTracked = true))
            val tracking =
                SelfUpdateTracking(trackedApps, settings, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

            tracking.start("SilberGecko6917/APK-Manager", "me.geckotv.apkmanager", "1.0.0")

            assertTrue(trackedApps.trackedApps.isEmpty())
        }
}
