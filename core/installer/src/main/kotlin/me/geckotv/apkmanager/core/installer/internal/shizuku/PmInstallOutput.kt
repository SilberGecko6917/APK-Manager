package me.geckotv.apkmanager.core.installer.internal.shizuku

import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.internal.SessionResult

/** Reads the output of `pm install`: "Success", or "Failure [INSTALL_FAILED_…: message]". */
internal object PmInstallOutput {
    private val failureCode = Regex("""\[([A-Z_]+)""")

    fun toSessionResult(output: String): SessionResult {
        val text = output.trim()
        if (text.lines().any { it.trim() == "Success" }) return SessionResult.Success
        val code =
            failureCode
                .find(text)
                ?.groupValues
                ?.get(1)
                .orEmpty()
        return SessionResult.Failure(failure(code, text))
    }

    private fun failure(
        code: String,
        message: String,
    ): InstallFailure =
        when {
            code in signatureCodes -> InstallFailure.SignatureMismatch
            code == "INSTALL_FAILED_INSUFFICIENT_STORAGE" -> InstallFailure.Storage
            code == "INSTALL_FAILED_INVALID_APK" || code.startsWith("INSTALL_PARSE_FAILED") -> InstallFailure.InvalidApk
            code in incompatibleCodes -> InstallFailure.Incompatible(message)
            "DEVELOPER_VERIFICATION" in code -> InstallFailure.DeveloperVerificationFailed(networkUnavailable = false)
            code in blockedCodes -> InstallFailure.Blocked(message)
            code == "INSTALL_FAILED_ABORTED" -> InstallFailure.Cancelled
            else -> InstallFailure.SystemError(message)
        }

    private val signatureCodes =
        setOf("INSTALL_FAILED_UPDATE_INCOMPATIBLE", "INSTALL_FAILED_SHARED_USER_INCOMPATIBLE")

    private val incompatibleCodes =
        setOf(
            "INSTALL_FAILED_OLDER_SDK",
            "INSTALL_FAILED_NEWER_SDK",
            "INSTALL_FAILED_NO_MATCHING_ABIS",
            "INSTALL_FAILED_MISSING_FEATURE",
            "INSTALL_FAILED_MISSING_SHARED_LIBRARY",
            "INSTALL_FAILED_DEPRECATED_SDK_VERSION",
        )

    private val blockedCodes =
        setOf(
            "INSTALL_FAILED_USER_RESTRICTED",
            "INSTALL_FAILED_VERIFICATION_FAILURE",
            "INSTALL_FAILED_VERIFICATION_TIMEOUT",
        )
}
