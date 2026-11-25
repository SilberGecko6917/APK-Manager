package me.geckotv.apkmanager.core.installer

import android.content.Intent
import android.content.pm.PackageInstaller
import me.geckotv.apkmanager.core.installer.internal.SessionResult
import me.geckotv.apkmanager.core.installer.internal.toSessionResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InstallStatusReceiverTest {
    @Test
    fun success() {
        assertEquals(SessionResult.Success, status(PackageInstaller.STATUS_SUCCESS).toSessionResult())
    }

    @Test
    fun pendingUserAction_carriesConfirmationIntent() {
        val confirmation = Intent("android.content.pm.action.CONFIRM_INSTALL")
        val intent = status(PackageInstaller.STATUS_PENDING_USER_ACTION).putExtra(Intent.EXTRA_INTENT, confirmation)

        val result = intent.toSessionResult() as SessionResult.PendingUserAction

        assertEquals("android.content.pm.action.CONFIRM_INSTALL", result.intent.action)
    }

    @Test
    fun failures_areMapped() {
        val cases =
            listOf(
                status(PackageInstaller.STATUS_FAILURE_ABORTED) to InstallFailure.Cancelled,
                status(PackageInstaller.STATUS_FAILURE_STORAGE) to InstallFailure.Storage,
                status(PackageInstaller.STATUS_FAILURE_BLOCKED, "policy") to InstallFailure.Blocked("policy"),
                status(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, "sdk") to InstallFailure.Incompatible("sdk"),
                status(PackageInstaller.STATUS_FAILURE_CONFLICT, "INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures") to
                    InstallFailure.SignatureMismatch,
                status(PackageInstaller.STATUS_FAILURE_INVALID, "INSTALL_PARSE_FAILED_NOT_APK") to
                    InstallFailure.InvalidApk,
                status(PackageInstaller.STATUS_FAILURE_INVALID, "INSTALL_FAILED_VERSION_DOWNGRADE") to
                    InstallFailure.SystemError("INSTALL_FAILED_VERSION_DOWNGRADE"),
                status(PackageInstaller.STATUS_FAILURE, "internal") to InstallFailure.SystemError("internal"),
            )

        cases.forEach { (intent, failure) ->
            assertEquals(SessionResult.Failure(failure), intent.toSessionResult())
        }
    }

    @Test
    fun developerVerificationFailure_isRecognized() {
        val blocked =
            status(PackageInstaller.STATUS_FAILURE_ABORTED)
                .putExtra("android.content.pm.extra.DEVELOPER_VERIFICATION_FAILURE_REASON", 2)
        val offline =
            status(PackageInstaller.STATUS_FAILURE_ABORTED)
                .putExtra("android.content.pm.extra.DEVELOPER_VERIFICATION_FAILURE_REASON", 1)

        assertEquals(
            SessionResult.Failure(InstallFailure.DeveloperVerificationFailed(networkUnavailable = false)),
            blocked.toSessionResult(),
        )
        assertEquals(
            SessionResult.Failure(InstallFailure.DeveloperVerificationFailed(networkUnavailable = true)),
            offline.toSessionResult(),
        )
    }

    private fun status(
        status: Int,
        message: String? = null,
    ) = Intent()
        .putExtra(PackageInstaller.EXTRA_STATUS, status)
        .putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, message)
}
