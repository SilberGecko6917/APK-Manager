package me.geckotv.apkmanager.core.database

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import me.geckotv.apkmanager.core.database.dao.TrackedAppDao
import me.geckotv.apkmanager.core.database.model.LatestReleaseEntity
import me.geckotv.apkmanager.core.database.model.ReleaseAssetEntity
import me.geckotv.apkmanager.core.database.model.TrackedAppEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackedAppDaoTest {
    private lateinit var database: ApkManagerDatabase
    private lateinit var dao: TrackedAppDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<ApkManagerDatabase>(
                    ApplicationProvider.getApplicationContext<Context>(),
                ).build()
        dao = database.trackedAppDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertedApp_isObservedWithoutRelease() =
        runTest {
            val id = dao.insertTrackedApp(trackedApp(owner = "octo", name = "app"))

            dao.observeTrackedApps().test {
                val apps = awaitItem()
                assertEquals(1, apps.size)
                assertEquals(id, apps.single().app.id)
                assertNull(apps.single().latestRelease)
                assertTrue(apps.single().assets.isEmpty())
            }
        }

    @Test
    fun findByRepository_ignoresCase() =
        runTest {
            dao.insertTrackedApp(trackedApp(owner = "Octo", name = "My-App"))

            assertNotNull(dao.findByRepository(owner = "octo", name = "my-app"))
        }

    @Test(expected = Exception::class)
    fun insertingSameRepositoryTwice_fails() =
        runTest {
            dao.insertTrackedApp(trackedApp(owner = "octo", name = "app"))
            dao.insertTrackedApp(trackedApp(owner = "OCTO", name = "APP"))
        }

    @Test
    fun replaceLatestRelease_replacesReleaseAndAssets() =
        runTest {
            val id = dao.insertTrackedApp(trackedApp(owner = "octo", name = "app"))
            dao.replaceLatestRelease(id, release(id, releaseId = 1, tag = "v1.0.0"), listOf(asset(id, assetId = 10)))

            dao.replaceLatestRelease(
                id,
                release(id, releaseId = 2, tag = "v1.1.0"),
                listOf(asset(id, assetId = 20), asset(id, assetId = 21)),
            )

            val app = dao.getTrackedApps().single()
            assertEquals("v1.1.0", app.latestRelease?.tagName)
            assertEquals(listOf(20L, 21L), app.assets.map { it.id }.sorted())
        }

    @Test
    fun replaceLatestReleaseWithNull_removesReleaseAndAssets() =
        runTest {
            val id = dao.insertTrackedApp(trackedApp(owner = "octo", name = "app"))
            dao.replaceLatestRelease(id, release(id, releaseId = 1, tag = "v1.0.0"), listOf(asset(id, assetId = 10)))

            dao.replaceLatestRelease(id, release = null, assets = emptyList())

            val app = dao.getTrackedApps().single()
            assertNull(app.latestRelease)
            assertTrue(app.assets.isEmpty())
        }

    @Test
    fun deletingApp_cascadesToReleaseAndAssets() =
        runTest {
            val id = dao.insertTrackedApp(trackedApp(owner = "octo", name = "app"))
            dao.replaceLatestRelease(id, release(id, releaseId = 1, tag = "v1.0.0"), listOf(asset(id, assetId = 10)))

            dao.deleteTrackedApp(id)

            assertTrue(dao.getTrackedApps().isEmpty())
            // The asset ID can be reused, so its row must be gone.
            val otherId = dao.insertTrackedApp(trackedApp(owner = "octo", name = "other"))
            dao.replaceLatestRelease(
                otherId,
                release(otherId, releaseId = 1, tag = "v1.0.0"),
                listOf(asset(otherId, assetId = 10)),
            )
        }

    @Test
    fun setters_updateColumns() =
        runTest {
            val id = dao.insertTrackedApp(trackedApp(owner = "octo", name = "app").copy(releaseEtag = "\"etag\""))

            dao.setPackageName(id, "com.example.app")
            dao.setPreferredAssetName(id, "app-arm64.apk")
            dao.setIncludePrereleases(id, includePrereleases = true)
            dao.setChecked(id, checkedAt = 42, etag = null)
            dao.setNotifiedRelease(id, releaseId = 7)
            dao.setAssetFilter(id, include = "foss", exclude = "debug")
            dao.setVersionPattern(id, pattern = "(\\d+)")
            dao.setSkippedRelease(id, releaseId = 9)
            dao.setUpdatePolicy(id, policy = "Install")
            dao.setAutoUpdateRelease(id, releaseId = 10)

            val app = dao.getTrackedApp(id)!!
            assertEquals("com.example.app", app.packageName)
            assertEquals("app-arm64.apk", app.preferredAssetName)
            assertTrue(app.includePrereleases)
            assertEquals(42L, app.lastCheckedAt)
            assertNull(app.releaseEtag)
            assertEquals(7L, app.notifiedReleaseId)
            assertEquals("foss", app.assetIncludePattern)
            assertEquals("debug", app.assetExcludePattern)
            assertEquals("(\\d+)", app.versionPattern)
            assertEquals(9L, app.skippedReleaseId)
            assertEquals("Install", app.updatePolicy)
            assertEquals(10L, app.autoUpdateReleaseId)

            dao.setInstalledRelease(id, releaseId = 11, versionCode = 42)

            val installed = dao.getTrackedApp(id)!!
            assertEquals(11L, installed.installedReleaseId)
            assertEquals(42L, installed.installedVersionCode)
            assertNull("Installing a release ends skipping", installed.skippedReleaseId)
        }

    private fun trackedApp(
        owner: String,
        name: String,
    ) = TrackedAppEntity(
        owner = owner,
        name = name,
        displayName = name,
        description = null,
        ownerAvatarUrl = null,
        isPrivate = false,
        packageName = null,
        includePrereleases = false,
        preferredAssetName = null,
        addedAt = 0,
        lastCheckedAt = null,
        releaseEtag = null,
    )

    private fun release(
        appId: Long,
        releaseId: Long,
        tag: String,
    ) = LatestReleaseEntity(
        appId = appId,
        releaseId = releaseId,
        tagName = tag,
        name = null,
        body = null,
        isPrerelease = false,
        publishedAt = null,
        htmlUrl = "https://github.com/octo/app/releases/tag/$tag",
    )

    private fun asset(
        appId: Long,
        assetId: Long,
    ) = ReleaseAssetEntity(
        id = assetId,
        appId = appId,
        name = "app-$assetId.apk",
        contentType = "application/vnd.android.package-archive",
        size = 1,
        sha256 = null,
        downloadCount = 0,
        browserDownloadUrl = "https://github.com/octo/app/releases/download/v1/app-$assetId.apk",
    )
}
