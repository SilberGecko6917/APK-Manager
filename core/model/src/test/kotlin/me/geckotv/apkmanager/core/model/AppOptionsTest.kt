package me.geckotv.apkmanager.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOptionsTest {
    @Test
    fun assetFilter_includeAndExclude() {
        val filter = AssetFilter(include = "foss", exclude = "debug|beta")

        assertTrue(filter.matches("app-foss-arm64.apk"))
        assertTrue(filter.matches("App-FOSS.apk"))
        assertFalse(filter.matches("app-gplay-arm64.apk"))
        assertFalse(filter.matches("app-foss-debug.apk"))
    }

    @Test
    fun assetFilter_emptyOrInvalidPatterns_matchEverything() {
        assertTrue(AssetFilter().matches("anything.apk"))
        assertTrue(AssetFilter().isEmpty)
        assertTrue(AssetFilter(include = "(unclosed", exclude = " ").matches("anything.apk"))
        assertFalse(AssetFilter(include = "(unclosed").isEmpty)
    }

    @Test
    fun versionFromTag_usesFirstGroupOrWholeMatch() {
        assertEquals("1.4.2", versionFromTag("release-build-1.4.2-final", """(\d+\.\d+\.\d+)"""))
        assertEquals("2024.05.01", versionFromTag("nightly-2024.05.01", """\d{4}\.\d{2}\.\d{2}"""))
    }

    @Test
    fun versionFromTag_withoutMatchingPattern_returnsTag() {
        assertEquals("v1.0", versionFromTag("v1.0", null))
        assertEquals("v1.0", versionFromTag("v1.0", "nomatch"))
        assertEquals("v1.0", versionFromTag("v1.0", "(unclosed"))
    }
}
