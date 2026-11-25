package me.geckotv.apkmanager.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    private fun v(text: String): Version = requireNotNull(Version.parse(text)) { "'$text' did not parse" }

    @Test
    fun `parses the first version number and ignores leading text`() {
        val cases =
            mapOf(
                "1" to "1",
                "1.2" to "1.2",
                "v1.2.3" to "1.2.3",
                "release-2024.05.01" to "2024.5.1",
                "App-v2.0.1" to "2.0.1",
                "MyApp_1.4" to "1.4",
                "1.2.3 (45)" to "1.2.3",
                "foo 3.1 and 4.2" to "3.1",
            )
        for ((input, expected) in cases) {
            assertEquals(input, expected, v(input).toString())
        }
    }

    @Test
    fun `returns null without digits`() {
        for (input in listOf("", "nightly", "latest", "v", "beta")) {
            assertNull(input, Version.parse(input))
        }
    }

    @Test
    fun `unknown suffixes and build metadata are ignored`() {
        for (suffix in listOf("-foss", " (45)", "+build.7", "-armeabi", "-development", "-alphabet", "-b")) {
            assertEquals(suffix, v("1.2.3"), v("1.2.3$suffix"))
        }
    }

    @Test
    fun `parses pre-release labels`() {
        val cases =
            mapOf(
                "1.2.3-beta.2" to "1.2.3-beta.2",
                "1.2.3beta2" to "1.2.3-beta.2",
                "1.2.3-RC1" to "1.2.3-rc.1",
                "1.2.3-alpha" to "1.2.3-alpha",
                "1.0a1" to "1.0-alpha.1",
                "1.0b2" to "1.0-beta.2",
                "1.2.3_preview-3" to "1.2.3-pre.3",
                "1.2.3.dev4" to "1.2.3-dev.4",
                "1.2.3-SNAPSHOT" to "1.2.3-dev",
                "1.2.3-pre" to "1.2.3-pre",
            )
        for ((input, expected) in cases) {
            assertEquals(input, expected, v(input).toString())
        }
    }

    @Test
    fun `missing trailing components are zero`() {
        assertEquals(v("1.2"), v("1.2.0"))
        assertEquals(v("1"), v("1.0.0.0"))
        assertEquals(v("1.2").hashCode(), v("1.2.0").hashCode())
        assertEquals(0, v("1.2").compareTo(v("1.2.0")))
        assertTrue(v("1.2") < v("1.2.1"))
    }

    @Test
    fun `numbers compare numerically`() {
        assertTrue(v("1.10.0") > v("1.9.0"))
        assertTrue(v("2.0") > v("1.99.99"))
        assertTrue(v("1.02") == v("1.2"))
        assertTrue(v("10") > v("9"))
    }

    @Test
    fun `long numbers do not overflow`() {
        assertTrue(v("20240501") > v("20240430"))
        assertTrue(v("1.99999999999999999999999") < v("1.100000000000000000000000"))
        assertTrue(v("123456789012345678901234567890") > v("123456789012345678901234567889"))
        assertEquals(v("00000000000000000000001"), v("1"))
    }

    @Test
    fun `pre-release is lower than the release`() {
        assertTrue(v("1.2.3-rc.1") < v("1.2.3"))
        assertTrue(v("1.2.3-alpha") < v("1.2.3"))
        assertTrue(v("1.2.3-rc.1") > v("1.2.2"))
        assertTrue(v("1.2.3-beta") < v("1.2.4-alpha"))
    }

    @Test
    fun `pre-release ranks are ordered`() {
        val ordered = listOf("1.0-dev", "1.0-alpha", "1.0-beta", "1.0-preview", "1.0-rc", "1.0")
        for (i in 0 until ordered.size - 1) {
            assertTrue("${ordered[i]} < ${ordered[i + 1]}", v(ordered[i]) < v(ordered[i + 1]))
        }
        assertEquals(v("1.0-snapshot"), v("1.0-dev"))
        assertEquals(v("1.0a1"), v("1.0-alpha.1"))
        assertEquals(v("1.0b2"), v("1.0-beta.2"))
        assertEquals(v("1.0-pre"), v("1.0-preview"))
    }

    @Test
    fun `pre-release numbers are compared and missing number is zero`() {
        assertTrue(v("1.0-beta.2") > v("1.0-beta.1"))
        assertTrue(v("1.0-beta.10") > v("1.0-beta.9"))
        assertTrue(v("1.0-beta.1") > v("1.0-beta"))
        assertEquals(v("1.0-beta"), v("1.0-beta.0"))
        assertEquals(v("1.0-beta2"), v("1.0-beta.2"))
    }

    @Test
    fun `equal versions have equal hash codes and sort stably`() {
        val a = v("v1.2.0-RC1")
        val b = v("1.2-rc.1")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotNull(Version.parse("x1"))
        val sorted = listOf("1.10", "1.2", "1.2-rc1", "1.9").map(::v).sorted().map { it.toString() }
        assertEquals(listOf("1.2-rc.1", "1.2", "1.9", "1.10"), sorted)
    }
}
