package me.geckotv.apkmanager.core.installer.internal

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.installer.InstallFailure
import java.io.File

/** What the system installer reports for a session. */
internal sealed interface SessionResult {
    data object Success : SessionResult

    /** The user has to confirm; [intent] opens the system dialog. More results follow. */
    data class PendingUserAction(
        val intent: Intent,
    ) : SessionResult

    data class Failure(
        val failure: InstallFailure,
    ) : SessionResult
}

/** Installs APK files through the system. */
internal interface SystemInstaller {
    /** Whether the user allowed APK Manager to install apps. */
    fun canInstallPackages(): Boolean

    /**
     * Installs [file] as [packageName]. Emits [SessionResult.PendingUserAction] while the user has to
     * confirm, and completes after a final [SessionResult.Success] or [SessionResult.Failure].
     * Cancelling the collection abandons the session.
     */
    fun install(
        file: File,
        packageName: String,
        label: String,
    ): Flow<SessionResult>
}
