package me.geckotv.apkmanager.core.installer

import me.geckotv.apkmanager.core.installer.internal.SessionResult
import me.geckotv.apkmanager.core.installer.internal.shizuku.PmInstallOutput
import org.junit.Assert.assertEquals
import org.junit.Test

class PmInstallOutputTest {
    @Test
    fun success() {
        assertEquals(SessionResult.Success, PmInstallOutput.toSessionResult("Success\n"))
        assertEquals(SessionResult.Success, PmInstallOutput.toSessionResult("Performing Streamed Install\nSuccess\n"))
    }

    @Test
    fun knownFailures_areMapped() {
        assertEquals(
            failure(InstallFailure.SignatureMismatch),
            PmInstallOutput.toSessionResult(
                "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package signatures do not match newer version]",
            ),
        )
        assertEquals(
            failure(InstallFailure.Storage),
            PmInstallOutput.toSessionResult("Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]"),
        )
        assertEquals(
            failure(InstallFailure.InvalidApk),
            PmInstallOutput.toSessionResult("Failure [INSTALL_PARSE_FAILED_NOT_APK: Failed to parse]"),
        )
        assertEquals(
            failure(InstallFailure.Cancelled),
            PmInstallOutput.toSessionResult("Failure [INSTALL_FAILED_ABORTED: Session was abandoned]"),
        )
    }

    @Test
    fun incompatibleAndBlocked_keepTheMessage() {
        val incompatible = "Failure [INSTALL_FAILED_NO_MATCHING_ABIS: Failed to extract native libraries]"
        val blocked = "Failure [INSTALL_FAILED_USER_RESTRICTED: Install canceled by user]"

        assertEquals(failure(InstallFailure.Incompatible(incompatible)), PmInstallOutput.toSessionResult(incompatible))
        assertEquals(failure(InstallFailure.Blocked(blocked)), PmInstallOutput.toSessionResult(blocked))
    }

    @Test
    fun anythingElse_isASystemError() {
        val output = "Error: java.lang.SecurityException: Shell does not have permission"

        assertEquals(failure(InstallFailure.SystemError(output)), PmInstallOutput.toSessionResult(output))
    }

    private fun failure(failure: InstallFailure) = SessionResult.Failure(failure)
}
