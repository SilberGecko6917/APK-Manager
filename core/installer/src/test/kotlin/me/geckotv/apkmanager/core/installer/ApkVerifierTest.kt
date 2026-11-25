package me.geckotv.apkmanager.core.installer

import me.geckotv.apkmanager.core.installer.internal.ApkInfo
import me.geckotv.apkmanager.core.installer.internal.ApkVerifier
import me.geckotv.apkmanager.core.installer.internal.DownloadedFile
import me.geckotv.apkmanager.core.installer.internal.InstalledPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApkVerifierTest {
    private val file = DownloadedFile(size = 100, sha256 = "abc", expectedSize = 100, expectedSha256 = "ABC")
    private val apk = apk(versionCode = 2, signers = setOf("key-a"))
    private val installed = InstalledPackage(versionCode = 1, signers = setOf("key-a"))

    @Test
    fun matchingUpdate_isAccepted() {
        assertNull(ApkVerifier.verify(file, apk, "com.example.app", installed))
    }

    @Test
    fun notInstalled_isAccepted() {
        assertNull(ApkVerifier.verify(file, apk, expectedPackageName = null, installed = null))
    }

    @Test
    fun unknownChecksumAndSize_areAccepted() {
        val withoutExpectations = file.copy(expectedSize = null, expectedSha256 = null)

        assertNull(ApkVerifier.verify(withoutExpectations, apk, "com.example.app", installed))
    }

    @Test
    fun wrongChecksum_isRejected() {
        assertEquals(
            InstallFailure.ChecksumMismatch,
            ApkVerifier.verify(file.copy(sha256 = "def"), apk, "com.example.app", installed),
        )
    }

    @Test
    fun wrongSize_isRejected() {
        assertEquals(
            InstallFailure.ChecksumMismatch,
            ApkVerifier.verify(file.copy(size = 99), apk, "com.example.app", installed),
        )
    }

    @Test
    fun unreadableApk_isRejected() {
        assertEquals(InstallFailure.InvalidApk, ApkVerifier.verify(file, apk = null, "com.example.app", installed))
        assertEquals(
            InstallFailure.InvalidApk,
            ApkVerifier.verify(file, apk.copy(signers = emptySet()), "com.example.app", installed),
        )
    }

    @Test
    fun otherPackage_isRejected() {
        assertEquals(
            InstallFailure.PackageMismatch(expected = "com.example.other", actual = "com.example.app"),
            ApkVerifier.verify(file, apk, "com.example.other", installed),
        )
    }

    @Test
    fun otherSigner_isRejected() {
        assertEquals(
            InstallFailure.SignatureMismatch,
            ApkVerifier.verify(file, apk(versionCode = 2, signers = setOf("key-b")), "com.example.app", installed),
        )
    }

    @Test
    fun rotatedKey_withInstalledKeyInLineage_isAccepted() {
        val rotated = apk(versionCode = 2, signers = setOf("key-b"), history = setOf("key-a", "key-b"))

        assertNull(ApkVerifier.verify(file, rotated, "com.example.app", installed))
    }

    @Test
    fun multipleSigners_mustBeEqual() {
        val installedTwoKeys = InstalledPackage(versionCode = 1, signers = setOf("key-a", "key-b"))

        assertNull(
            ApkVerifier.verify(file, apk(2, setOf("key-a", "key-b")), "com.example.app", installedTwoKeys),
        )
        assertEquals(
            InstallFailure.SignatureMismatch,
            ApkVerifier.verify(file, apk(2, setOf("key-a")), "com.example.app", installedTwoKeys),
        )
    }

    @Test
    fun downgrade_isRejected() {
        assertEquals(
            InstallFailure.Downgrade(installedVersionCode = 3, apkVersionCode = 2),
            ApkVerifier.verify(file, apk, "com.example.app", installed.copy(versionCode = 3)),
        )
    }

    @Test
    fun sameVersion_isAccepted() {
        assertNull(ApkVerifier.verify(file, apk, "com.example.app", installed.copy(versionCode = 2)))
    }

    private fun apk(
        versionCode: Long,
        signers: Set<String>,
        history: Set<String> = signers,
    ) = ApkInfo(
        packageName = "com.example.app",
        versionCode = versionCode,
        versionName = "1.$versionCode",
        signers = signers,
        signingHistory = history,
    )
}
