package me.geckotv.apkmanager.core.installer.internal

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/** Reads package name, version and signing certificates of APK files and installed apps. */
internal interface PackageInspector {
    /** What [file] contains, or `null` if it is not a valid APK. */
    fun inspectApk(file: File): ApkInfo?

    /** The installed version of [packageName], or `null` if it is not installed. */
    fun installedPackage(packageName: String): InstalledPackage?
}

internal class PackageManagerInspector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PackageInspector {
        private val packageManager: PackageManager get() = context.packageManager

        override fun inspectApk(file: File): ApkInfo? {
            val info = packageManager.archiveInfo(file) ?: return null
            val (signers, history) = info.certificates()
            return ApkInfo(
                packageName = info.packageName,
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                versionName = info.versionName,
                signers = signers,
                signingHistory = history,
            )
        }

        override fun installedPackage(packageName: String): InstalledPackage? {
            val info =
                try {
                    packageManager.installedInfo(packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    return null
                }
            return InstalledPackage(
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                signers = info.certificates().first,
            )
        }

        /** The current signers and the whole signing lineage, as SHA-256 digests. */
        private fun PackageInfo.certificates(): Pair<Set<String>, Set<String>> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                signingCertificates()
            } else {
                @Suppress("DEPRECATION")
                val signers = signatures.orEmpty().digests()
                signers to signers
            }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun PackageInfo.signingCertificates(): Pair<Set<String>, Set<String>> {
            val signingInfo = signingInfo
            val signers = signingInfo?.apkContentsSigners.orEmpty().digests()
            val history =
                if (signingInfo == null || signingInfo.hasMultipleSigners()) {
                    signers
                } else {
                    signingInfo.signingCertificateHistory.orEmpty().digests() + signers
                }
            return signers to history
        }

        private fun Array<out Signature>.digests(): Set<String> = mapTo(mutableSetOf()) { it.toByteArray().sha256() }

        private fun PackageManager.archiveInfo(file: File): PackageInfo? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(signingFlags().toLong()))
            } else {
                getPackageArchiveInfo(file.path, signingFlags())
            }

        private fun PackageManager.installedInfo(packageName: String): PackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(signingFlags().toLong()))
            } else {
                getPackageInfo(packageName, signingFlags())
            }

        // GET_SIGNATURES is only used before API 28, where GET_SIGNING_CERTIFICATES does not exist.
        @SuppressLint("PackageManagerGetSignatures")
        @Suppress("DEPRECATION")
        private fun signingFlags(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
    }

/** Lowercase hex SHA-256 digest. */
internal fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHexString()
