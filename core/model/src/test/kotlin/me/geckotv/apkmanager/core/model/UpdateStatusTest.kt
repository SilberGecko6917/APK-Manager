package me.geckotv.apkmanager.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStatusTest {
    private fun installed(versionName: String?) =
        InstalledApp(
            packageName = "com.example.app",
            label = "Example",
            versionName = versionName,
            versionCode = 1,
            isSystemApp = false,
        )

    private fun release(tag: String) =
        Release(
            id = 1,
            tagName = tag,
            name = null,
            body = null,
            isPrerelease = false,
            publishedAt = null,
            htmlUrl = "https://example.com/release",
            assets = emptyList(),
        )

    private fun status(
        installedName: String?,
        tag: String,
    ) = UpdateStatus.of(installed(installedName), release(tag))

    @Test
    fun `not installed is checked first`() {
        assertEquals(UpdateStatus.NotInstalled, UpdateStatus.of(null, release("v1.0")))
        assertEquals(UpdateStatus.NotInstalled, UpdateStatus.of(null, null))
    }

    @Test
    fun `installed without release has no release`() {
        assertEquals(UpdateStatus.NoRelease, UpdateStatus.of(installed("1.0"), null))
    }

    @Test
    fun `newer release is an available update`() {
        assertEquals(UpdateStatus.UpdateAvailable, status("1.2.3", "v1.2.4"))
        assertEquals(UpdateStatus.UpdateAvailable, status("1.9", "v1.10"))
        assertEquals(UpdateStatus.UpdateAvailable, status("1.2.3-beta.1", "1.2.3"))
        assertEquals(UpdateStatus.UpdateAvailable, status("2024.05.01", "release-2024.06.01"))
    }

    @Test
    fun `same or older release is up to date`() {
        assertEquals(UpdateStatus.UpToDate, status("1.2.3", "v1.2.3"))
        assertEquals(UpdateStatus.UpToDate, status("1.2", "v1.2.0"))
        assertEquals(UpdateStatus.UpToDate, status("1.3.0", "v1.2.9"))
        assertEquals(UpdateStatus.UpToDate, status("1.2.3 (45)-foss", "v1.2.3"))
        assertEquals(UpdateStatus.UpToDate, status("1.2.3", "v1.2.3-rc.1"))
    }

    @Test
    fun `unparseable versions are up to date when normalized strings match`() {
        assertEquals(UpdateStatus.UpToDate, status("nightly", "nightly"))
        assertEquals(UpdateStatus.UpToDate, status("nightly-main", "v-Nightly_main"))
        assertEquals(UpdateStatus.UpToDate, status(" Latest ", "latest"))
        assertEquals(UpdateStatus.UpToDate, status("night ly", "night.ly"))
    }

    @Test
    fun `unparseable versions that differ are unknown`() {
        assertEquals(UpdateStatus.Unknown, status("nightly", "latest"))
        assertEquals(UpdateStatus.Unknown, status("nightly", "v1.0"))
        assertEquals(UpdateStatus.Unknown, status("1.0", "nightly"))
    }

    @Test
    fun `missing version name is treated as unparseable`() {
        assertEquals(UpdateStatus.Unknown, status(null, "v1.0"))
        assertEquals(UpdateStatus.Unknown, status(null, "nightly"))
    }
}
