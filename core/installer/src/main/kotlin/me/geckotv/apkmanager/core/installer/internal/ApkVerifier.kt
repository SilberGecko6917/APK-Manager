package me.geckotv.apkmanager.core.installer.internal

import me.geckotv.apkmanager.core.installer.InstallFailure

/** What an APK file contains, read from its manifest and signature. */
internal data class ApkInfo(
    val packageName: String,
    val versionCode: Long,
    val versionName: String?,
    /** SHA-256 digests (lowercase hex) of the certificates that sign the APK now. */
    val signers: Set<String>,
    /**
     * SHA-256 digests of all certificates in the APK's signing lineage (APK Signature Scheme v3 key
     * rotation), including [signers]. Equal to [signers] without rotation.
     */
    val signingHistory: Set<String>,
)

/** The installed version of an app, for comparing it with a downloaded APK. */
internal data class InstalledPackage(
    val versionCode: Long,
    /** SHA-256 digests of the certificates that sign the installed app now. */
    val signers: Set<String>,
)

/** The file that was downloaded and what GitHub said it should be. */
internal data class DownloadedFile(
    val size: Long,
    val sha256: String,
    val expectedSize: Long?,
    val expectedSha256: String?,
)

/**
 * Decides whether a downloaded APK may be installed as an update of a tracked app. Android would
 * reject a different signer or a downgrade too, but checking first gives clear messages and avoids
 * installing an unexpected app.
 */
internal object ApkVerifier {
    /**
     * @param expectedPackageName the package name the tracked app is linked to, `null` if unknown.
     * @param installed the installed version of the APK's package, `null` if it is not installed.
     * @return why the APK must not be installed, or `null` if it may.
     */
    fun verify(
        file: DownloadedFile,
        apk: ApkInfo?,
        expectedPackageName: String?,
        installed: InstalledPackage?,
    ): InstallFailure? =
        when {
            !file.matchesExpectation() -> {
                InstallFailure.ChecksumMismatch
            }

            apk == null || apk.signers.isEmpty() -> {
                InstallFailure.InvalidApk
            }

            expectedPackageName != null && apk.packageName != expectedPackageName -> {
                InstallFailure.PackageMismatch(expected = expectedPackageName, actual = apk.packageName)
            }

            installed == null -> {
                null
            }

            !isSignedCompatibly(apk, installed) -> {
                InstallFailure.SignatureMismatch
            }

            apk.versionCode < installed.versionCode -> {
                InstallFailure.Downgrade(installedVersionCode = installed.versionCode, apkVersionCode = apk.versionCode)
            }

            else -> {
                null
            }
        }

    private fun DownloadedFile.matchesExpectation(): Boolean =
        (expectedSize == null || expectedSize == size) &&
            (expectedSha256 == null || expectedSha256.equals(sha256, ignoreCase = true))

    /**
     * Android accepts an update if the installed app's signer is in the APK's signing lineage (key
     * rotation), or, for apps signed by several keys, if the signer sets are equal.
     */
    private fun isSignedCompatibly(
        apk: ApkInfo,
        installed: InstalledPackage,
    ): Boolean =
        if (installed.signers.size > 1 || apk.signers.size > 1) {
            installed.signers == apk.signers
        } else {
            installed.signers.isNotEmpty() && apk.signingHistory.containsAll(installed.signers)
        }
}
