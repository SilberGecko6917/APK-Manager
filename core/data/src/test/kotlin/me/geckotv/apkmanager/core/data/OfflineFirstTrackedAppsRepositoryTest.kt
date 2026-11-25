package me.geckotv.apkmanager.core.data

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.data.internal.OfflineFirstTrackedAppsRepository
import me.geckotv.apkmanager.core.data.internal.ReleaseChecker
import me.geckotv.apkmanager.core.data.repository.AddAppOptions
import me.geckotv.apkmanager.core.database.ApkManagerDatabase
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.core.model.SourceResult
import me.geckotv.apkmanager.core.model.UpdateStatus
import me.geckotv.apkmanager.core.network.model.GitHubError
import me.geckotv.apkmanager.core.network.model.RepositoryRef
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class OfflineFirstTrackedAppsRepositoryTest {
    private val ref = RepositoryRef("octo", "app")
    private val repository = GitHubRepository("octo", "app")
    private val clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC)

    private lateinit var database: ApkManagerDatabase
    private val network = FakeGitHubNetworkDataSource()
    private val installedApps = FakeInstalledAppsRepository()
    private var token: String? = null

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<ApkManagerDatabase>(
                    ApplicationProvider.getApplicationContext<Context>(),
                ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun TestScope.repository(): OfflineFirstTrackedAppsRepository {
        val dao = database.trackedAppDao()
        return OfflineFirstTrackedAppsRepository(
            dao = dao,
            network = network,
            installedApps = installedApps,
            releaseChecker = ReleaseChecker(network, { token }, dao, clock),
            clock = clock,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    @Test
    fun lookUpRepository_returnsRepositoryWithPublishedReleases() =
        runTest {
            network.addRepository(
                ref,
                networkRelease(3, "v2.0.0-beta.1", prerelease = true),
                networkRelease(2, "v1.1.0", draft = true),
                networkRelease(1, "v1.0.0"),
            )

            val preview = (repository().lookUpRepository(repository) as SourceResult.Success).data

            assertEquals(repository, preview.repository)
            assertEquals(listOf("v2.0.0-beta.1", "v1.0.0"), preview.releases.map { it.tagName })
            assertEquals("v1.0.0", preview.latestRelease(includePrereleases = false)?.tagName)
            assertEquals("v2.0.0-beta.1", preview.latestRelease(includePrereleases = true)?.tagName)
            assertEquals(
                "abc0",
                preview.releases
                    .last()
                    .assets
                    .single()
                    .sha256,
            )
        }

    @Test
    fun lookUpRepository_withoutReleases_returnsEmptyReleases() =
        runTest {
            network.addRepository(ref)
            network.releases.remove(ref)

            val preview = (repository().lookUpRepository(repository) as SourceResult.Success).data

            assertTrue(preview.releases.isEmpty())
        }

    @Test
    fun lookUpRepository_unknownRepository_failsWithNotFound() =
        runTest {
            assertEquals(SourceResult.Failure(SourceError.NotFound), repository().lookUpRepository(repository))
        }

    @Test
    fun addApp_storesAppWithLatestReleaseAndInstalledVersion() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.1.0"))
            installedApps.install(installedApp(versionName = "1.0.0"))
            val repository = repository()

            val id = repository.addApp(lookUp(repository), AddAppOptions("com.example.app", false, null))

            repository.observeTrackedApp(id).test {
                val app = awaitItem()!!
                assertEquals("Example", app.displayName)
                assertEquals("v1.1.0", app.latestRelease?.tagName)
                assertEquals(UpdateStatus.UpdateAvailable, app.updateStatus)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addApp_twice_returnsExistingApp() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            val preview = lookUp(repository)

            val first = repository.addApp(preview, AddAppOptions(null, false, null))
            val second = repository.addApp(preview, AddAppOptions(null, true, null))

            assertEquals(first, second)
            assertEquals(first, repository.findTrackedApp(GitHubRepository("OCTO", "App")))
        }

    @Test
    fun installingTrackedApp_updatesObservedStatus() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            repository.addApp(lookUp(repository), AddAppOptions("com.example.app", false, null))

            repository.observeTrackedApps().test {
                assertEquals(UpdateStatus.NotInstalled, awaitItem().single().updateStatus)

                installedApps.install(installedApp(versionName = "1.0.0"))

                assertEquals(UpdateStatus.UpToDate, awaitItem().single().updateStatus)
            }
        }

    @Test
    fun checkForUpdates_signedOut_usesLatestReleaseWithEtag() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            val id = repository.addApp(lookUp(repository), AddAppOptions(null, false, null))
            network.calls.clear()

            // The first check has no ETag yet; the second one sends it and gets 304.
            assertTrue(repository.checkForUpdates().isSuccess)
            assertTrue(repository.checkForUpdates().isSuccess)

            assertEquals(
                listOf("latestRelease $ref etag=null", "latestRelease $ref etag=\"etag-1\""),
                network.calls,
            )
            assertEquals(
                "v1.0.0",
                database
                    .trackedAppDao()
                    .getTrackedApps()
                    .single()
                    .latestRelease
                    ?.tagName,
            )
            assertEquals(clock.millis(), database.trackedAppDao().getTrackedApp(id)?.lastCheckedAt)
        }

    @Test
    fun checkForUpdates_signedOut_storesNewRelease() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            repository.addApp(lookUp(repository), AddAppOptions(null, false, null))
            network.releases[ref] = listOf(networkRelease(2, "v1.1.0", assetNames = listOf("a.apk", "b.apk")))
            network.etag = "\"etag-2\""

            repository.checkForUpdates()

            val app = database.trackedAppDao().getTrackedApps().single()
            assertEquals("v1.1.0", app.latestRelease?.tagName)
            assertEquals(listOf("a.apk", "b.apk"), app.assets.sortedBy { it.id }.map { it.name })
        }

    @Test
    fun checkForUpdates_signedIn_fetchesOnlyChangedReleases() =
        runTest {
            token = "token"
            val other = RepositoryRef("octo", "other")
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            network.addRepository(other, networkRelease(10, "v3.0.0"))
            val repository = repository()
            repository.addApp(lookUp(repository), AddAppOptions(null, false, null))
            repository.addApp(lookUp(repository, GitHubRepository("octo", "other")), AddAppOptions(null, false, null))
            network.releases[other] = listOf(networkRelease(11, "v3.1.0"))
            network.calls.clear()

            assertTrue(repository.checkForUpdates().isSuccess)

            assertEquals(
                listOf("latestReleases $ref, $other", "latestRelease $other etag=null"),
                network.calls,
            )
            assertEquals(
                setOf("v1.0.0", "v3.1.0"),
                database
                    .trackedAppDao()
                    .getTrackedApps()
                    .mapNotNull { it.latestRelease?.tagName }
                    .toSet(),
            )
        }

    @Test
    fun checkForUpdates_withPrereleases_usesReleaseList() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            repository.addApp(lookUp(repository), AddAppOptions(null, true, null))
            network.releases[ref] =
                listOf(networkRelease(2, "v1.1.0-rc.1", prerelease = true), networkRelease(1, "v1.0.0"))
            network.etag = "\"etag-2\""
            network.calls.clear()

            repository.checkForUpdates()

            assertEquals(listOf("releases $ref etag=null"), network.calls)
            assertEquals(
                "v1.1.0-rc.1",
                database
                    .trackedAppDao()
                    .getTrackedApps()
                    .single()
                    .latestRelease
                    ?.tagName,
            )
        }

    @Test
    fun checkForUpdates_releaseRemoved_clearsRelease() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            repository.addApp(lookUp(repository), AddAppOptions(null, false, null))
            network.releases[ref] = emptyList()

            assertTrue(repository.checkForUpdates().isSuccess)

            assertNull(
                database
                    .trackedAppDao()
                    .getTrackedApps()
                    .single()
                    .latestRelease,
            )
        }

    @Test
    fun checkForUpdates_rateLimited_stopsAndReportsAllApps() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            network.addRepository(RepositoryRef("octo", "other"), networkRelease(10, "v3.0.0"))
            val repository = repository()
            repository.addApp(lookUp(repository), AddAppOptions(null, false, null))
            repository.addApp(lookUp(repository, GitHubRepository("octo", "other")), AddAppOptions(null, false, null))
            network.error = GitHubError.RateLimited(retryAt = null)
            network.calls.clear()

            val result = repository.checkForUpdates()

            assertEquals(0, result.checkedCount)
            assertEquals(setOf(SourceError.RateLimited(null)), result.failures.values.toSet())
            assertEquals(2, result.failures.size)
            assertEquals(1, network.calls.size)
        }

    @Test
    fun setIncludePrereleases_checksAppAgain() =
        runTest {
            network.addRepository(
                ref,
                networkRelease(2, "v2.0.0-beta.1", prerelease = true),
                networkRelease(1, "v1.0.0"),
            )
            val repository = repository()
            val id = repository.addApp(lookUp(repository), AddAppOptions(null, false, null))

            repository.setIncludePrereleases(id, includePrereleases = true)

            assertEquals(
                "v2.0.0-beta.1",
                database
                    .trackedAppDao()
                    .getTrackedApps()
                    .single()
                    .latestRelease
                    ?.tagName,
            )
        }

    @Test
    fun removeApp_deletesIt() =
        runTest {
            network.addRepository(ref, networkRelease(1, "v1.0.0"))
            val repository = repository()
            val id = repository.addApp(lookUp(repository), AddAppOptions(null, false, null))

            repository.removeApp(id)

            assertTrue(database.trackedAppDao().getTrackedApps().isEmpty())
        }

    private suspend fun lookUp(
        repository: OfflineFirstTrackedAppsRepository,
        gitHubRepository: GitHubRepository = this.repository,
    ): RepositoryPreview = (repository.lookUpRepository(gitHubRepository) as SourceResult.Success).data

    private fun installedApp(versionName: String) =
        InstalledApp(
            packageName = "com.example.app",
            label = "Example",
            versionName = versionName,
            versionCode = 1,
            isSystemApp = false,
        )
}
