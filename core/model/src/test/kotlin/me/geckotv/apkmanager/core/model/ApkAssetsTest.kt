package me.geckotv.apkmanager.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkAssetsTest {
    private val arm64Device = listOf("arm64-v8a", "armeabi-v7a", "armeabi")

    private fun asset(
        name: String,
        id: Long = name.hashCode().toLong(),
    ) = ReleaseAsset(
        id = id,
        name = name,
        contentType = null,
        size = 1,
        sha256 = null,
        downloadCount = 0,
        browserDownloadUrl = "https://example.com/$name",
    )

    private fun names(
        vararg assetNames: String,
        abis: List<String> = arm64Device,
    ) = ApkAssets.candidates(assetNames.map { asset(it) }, abis).map { it.asset.name }

    private fun abiOf(name: String): String? = ApkAssets.candidates(listOf(asset(name)), emptyList()).single().abi

    @Test
    fun `detects and normalizes abis from file names`() {
        val cases =
            mapOf(
                "app-arm64-v8a.apk" to "arm64-v8a",
                "app-arm64.apk" to "arm64-v8a",
                "app-aarch64.apk" to "arm64-v8a",
                "app_armv8.apk" to "arm64-v8a",
                "app-armeabi-v7a.apk" to "armeabi-v7a",
                "app-armeabi.apk" to "armeabi-v7a",
                "app-armv7.apk" to "armeabi-v7a",
                "app-armv7a.apk" to "armeabi-v7a",
                "app-arm32.apk" to "armeabi-v7a",
                "app-arm.apk" to "armeabi-v7a",
                "app-x86_64.apk" to "x86_64",
                "app-x86-64.apk" to "x86_64",
                "app.x64.apk" to "x86_64",
                "app-amd64.apk" to "x86_64",
                "app-x86.apk" to "x86",
                "app-i686.apk" to "x86",
                "app-i386.apk" to "x86",
                "App-ARM64-V8A.APK" to "arm64-v8a",
                "app-1.2.3-arm64.apk" to "arm64-v8a",
            )
        for ((name, abi) in cases) {
            assertEquals(name, abi, abiOf(name))
        }
    }

    @Test
    fun `universal names have no abi`() {
        for (name in listOf("app.apk", "app-universal.apk", "app-all.apk", "app-fat.apk", "app-1.0.apk")) {
            assertNull(name, abiOf(name))
        }
    }

    @Test
    fun `abi tokens are not matched inside words`() {
        for (name in listOf("farm.apk", "alarm-1.0.apk", "charm.apk", "pharma-x86x.apk", "app-armor.apk")) {
            assertNull(name, abiOf(name))
        }
    }

    @Test
    fun `only apk files are candidates`() {
        val result =
            names(
                "app.apk.sha256",
                "app.aab",
                "app.apks",
                "app.zip",
                "app-universal.APK",
                "checksums.txt",
            )
        assertEquals(listOf("app-universal.APK"), result)
    }

    @Test
    fun `compatibility depends on the supported abis`() {
        val candidates =
            ApkAssets
                .candidates(
                    listOf(asset("a-arm64-v8a.apk"), asset("a-x86_64.apk"), asset("a-universal.apk")),
                    arm64Device,
                ).associateBy { it.asset.name }
        assertTrue(candidates.getValue("a-arm64-v8a.apk").isCompatible)
        assertFalse(candidates.getValue("a-x86_64.apk").isCompatible)
        assertTrue(candidates.getValue("a-universal.apk").isCompatible)
    }

    @Test
    fun `debug builds are detected by token`() {
        val candidates =
            ApkAssets
                .candidates(
                    listOf(asset("app-debug.apk"), asset("app-release.apk"), asset("app-debugger.apk")),
                    arm64Device,
                ).associate { it.asset.name to it.isDebug }
        assertEquals(true, candidates["app-debug.apk"])
        assertEquals(false, candidates["app-release.apk"])
        assertEquals(false, candidates["app-debugger.apk"])
    }

    @Test
    fun `candidates are ordered by compatibility, abi preference and build type`() {
        val result =
            names(
                "app-x86_64.apk",
                "app-universal.apk",
                "app-debug-arm64-v8a.apk",
                "app-armeabi-v7a.apk",
                "app-arm64-v8a.apk",
                "app-debug.apk",
            )
        assertEquals(
            listOf(
                "app-arm64-v8a.apk",
                "app-armeabi-v7a.apk",
                "app-universal.apk",
                "app-debug-arm64-v8a.apk",
                "app-debug.apk",
                "app-x86_64.apk",
            ),
            result,
        )
    }

    @Test
    fun `candidates keep the original order for ties`() {
        assertEquals(listOf("b.apk", "a.apk", "c.apk"), names("b.apk", "a.apk", "c.apk"))
    }

    @Test
    fun `select returns the best compatible release build`() {
        val assets =
            listOf(
                asset("app-x86_64.apk"),
                asset("app-debug.apk"),
                asset("app-universal.apk"),
                asset("app-arm64-v8a.apk"),
            )
        assertEquals("app-arm64-v8a.apk", ApkAssets.select(assets, arm64Device, null)?.name)
        assertEquals("app-universal.apk", ApkAssets.select(assets, listOf("riscv64"), null)?.name)
    }

    @Test
    fun `select falls back to a debug build and then to null`() {
        val debugOnly = listOf(asset("app-x86.apk"), asset("app-debug.apk"))
        assertEquals("app-debug.apk", ApkAssets.select(debugOnly, arm64Device, null)?.name)
        assertNull(ApkAssets.select(listOf(asset("app-x86.apk"), asset("notes.txt")), arm64Device, null))
        assertNull(ApkAssets.select(emptyList(), arm64Device, "app.apk"))
    }

    @Test
    fun `select prefers the asset matching the preferred name`() {
        val assets =
            listOf(
                asset("app-v1.3.1-arm64-v8a.apk"),
                asset("app-v1.3.1-universal.apk"),
                asset("app-v1.3.1-armeabi-v7a.apk"),
            )
        assertEquals(
            "app-v1.3.1-universal.apk",
            ApkAssets.select(assets, arm64Device, "app-v1.2.0-universal.apk")?.name,
        )
    }

    @Test
    fun `select ignores a preferred asset that is incompatible or missing`() {
        val assets = listOf(asset("app-v2-x86_64.apk"), asset("app-v2-arm64-v8a.apk"))
        assertEquals("app-v2-arm64-v8a.apk", ApkAssets.select(assets, arm64Device, "app-v1-x86_64.apk")?.name)
        assertEquals("app-v2-arm64-v8a.apk", ApkAssets.select(assets, arm64Device, "other-v1.apk")?.name)
    }

    @Test
    fun `matchesPreferred ignores versions`() {
        val matching =
            listOf(
                "app-v1.2.0-arm64.apk" to "app-v1.3.1-arm64.apk",
                "app-1.2.0-arm64-v8a.apk" to "app-1.10.3-arm64-v8a.apk",
                "App_1.2_universal.apk" to "app_v2.0_universal.apk",
                "app-2024.05.01.apk" to "app-2024.06.15.apk",
                "app-1.0.0-x86_64.apk" to "app-1.0.1-x86_64.apk",
                "app.apk" to "app.apk",
                "app-1.0.apk" to "APP-2.0.APK",
            )
        for ((a, b) in matching) {
            assertTrue("$a ~ $b", ApkAssets.matchesPreferred(a, b))
            assertTrue("$b ~ $a", ApkAssets.matchesPreferred(b, a))
        }
    }

    @Test
    fun `matchesPreferred keeps abis apart`() {
        val different =
            listOf(
                "app-1.2.0-arm64-v8a.apk" to "app-1.2.0-armeabi-v7a.apk",
                "app-1.2.0-x86.apk" to "app-1.2.0-x86_64.apk",
                "app-1.2.0-arm64-v8a.apk" to "app-1.2.0-x86_64.apk",
                "app-1.2.0-arm64-v8a.apk" to "app-1.2.0-universal.apk",
                "app-1.2.0.apk" to "app-1.2.0-debug.apk",
                "app-foo-1.0.apk" to "app-bar-1.0.apk",
            )
        for ((a, b) in different) {
            assertFalse("$a !~ $b", ApkAssets.matchesPreferred(a, b))
        }
    }
}
