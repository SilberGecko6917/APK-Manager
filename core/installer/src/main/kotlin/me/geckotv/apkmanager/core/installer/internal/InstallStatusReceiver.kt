package me.geckotv.apkmanager.core.installer.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import me.geckotv.apkmanager.core.installer.InstallFailure

/**
 * Receives the status of install sessions from the system and publishes it in [results], by session
 * ID. A state map (not an event stream) is used, so a result that arrives before anyone listens is
 * not lost.
 */
internal class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (sessionId == -1) return
        val result = intent.toSessionResult()
        mutableResults.update { it + (sessionId to result) }
    }

    companion object {
        private val mutableResults = MutableStateFlow<Map<Int, SessionResult>>(emptyMap())

        /** The latest result of each session that has not been [consumed][consume]. */
        val results: StateFlow<Map<Int, SessionResult>> = mutableResults

        fun consume(sessionId: Int) {
            mutableResults.update { it - sessionId }
        }
    }
}

/** Key of `PackageInstaller.EXTRA_DEVELOPER_VERIFICATION_FAILURE_REASON` (Android 16 QPR2+). */
private const val EXTRA_DEVELOPER_VERIFICATION_FAILURE_REASON =
    "android.content.pm.extra.DEVELOPER_VERIFICATION_FAILURE_REASON"

/** `PackageInstaller.DEVELOPER_VERIFICATION_FAILED_REASON_NETWORK_UNAVAILABLE`. */
private const val DEVELOPER_VERIFICATION_NETWORK_UNAVAILABLE = 1

internal fun Intent.toSessionResult(): SessionResult {
    val message = getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
    return when (val status = getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
        PackageInstaller.STATUS_SUCCESS -> {
            SessionResult.Success
        }

        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
            IntentCompat
                .getParcelableExtra(this, Intent.EXTRA_INTENT, Intent::class.java)
                ?.let { SessionResult.PendingUserAction(it) }
                ?: SessionResult.Failure(InstallFailure.SystemError(message))
        }

        else -> {
            SessionResult.Failure(failure(status, message))
        }
    }
}

private fun Intent.failure(
    status: Int,
    message: String?,
): InstallFailure {
    val verificationReason = getIntExtra(EXTRA_DEVELOPER_VERIFICATION_FAILURE_REASON, -1)
    return when {
        verificationReason >= 0 -> {
            InstallFailure.DeveloperVerificationFailed(
                networkUnavailable = verificationReason == DEVELOPER_VERIFICATION_NETWORK_UNAVAILABLE,
            )
        }

        status == PackageInstaller.STATUS_FAILURE_ABORTED -> {
            InstallFailure.Cancelled
        }

        status == PackageInstaller.STATUS_FAILURE_BLOCKED -> {
            InstallFailure.Blocked(message)
        }

        status == PackageInstaller.STATUS_FAILURE_STORAGE -> {
            InstallFailure.Storage
        }

        status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
            InstallFailure.Incompatible(message)
        }

        // INSTALL_FAILED_UPDATE_INCOMPATIBLE: the APK is signed with a different key.
        status == PackageInstaller.STATUS_FAILURE_CONFLICT && message.orEmpty().contains("UPDATE_INCOMPATIBLE") -> {
            InstallFailure.SignatureMismatch
        }

        status == PackageInstaller.STATUS_FAILURE_INVALID && !message.orEmpty().contains("DOWNGRADE") -> {
            InstallFailure.InvalidApk
        }

        else -> {
            InstallFailure.SystemError(message)
        }
    }
}

/** Whether this Android version knows `setRequireUserAction`. */
internal val supportsUnattendedUpdates: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
