package me.geckotv.apkmanager.core.installer

import android.content.Intent
import me.geckotv.apkmanager.core.model.SourceError

/** Progress of installing the latest release of a tracked app. */
sealed interface InstallState {
    /** The APK is being downloaded; [totalBytes] is `null` if the size is unknown. */
    data class Downloading(
        val bytesRead: Long,
        val totalBytes: Long?,
    ) : InstallState {
        /** Progress from 0 to 1, or `null` if the size is unknown. */
        val progress: Float? get() = totalBytes?.takeIf { it > 0 }?.let { bytesRead.toFloat() / it }
    }

    /** The download is checked: checksum, package name, version and signing certificate. */
    data object Verifying : InstallState

    /** The system installs the APK. */
    data object Installing : InstallState

    /**
     * The system asks the user to confirm the installation. The UI launches [intent] (once) while the
     * app is in the foreground.
     */
    data class WaitingForUser(
        val intent: Intent,
    ) : InstallState

    /** The app was installed. */
    data class Succeeded(
        val versionName: String?,
    ) : InstallState

    data class Failed(
        val failure: InstallFailure,
    ) : InstallState

    /** Whether the install is still running. */
    val isActive: Boolean get() = this !is Succeeded && this !is Failed
}

/** Why an app could not be installed. */
sealed interface InstallFailure {
    /** The latest release has no APK for this device. */
    data object NoCompatibleApk : InstallFailure

    /** The user has not allowed APK Manager to install apps ("Install unknown apps"). */
    data object PermissionMissing : InstallFailure

    data class DownloadFailed(
        val error: SourceError,
    ) : InstallFailure

    /** The download does not match the size or SHA-256 checksum GitHub reported. */
    data object ChecksumMismatch : InstallFailure

    /** The file is not a valid APK. */
    data object InvalidApk : InstallFailure

    /** The APK is a different app than the tracked one. */
    data class PackageMismatch(
        val expected: String,
        val actual: String,
    ) : InstallFailure

    /** The APK is signed with a different key than the installed app, so Android would refuse the update. */
    data object SignatureMismatch : InstallFailure

    /** The APK is older than the installed app. */
    data class Downgrade(
        val installedVersionCode: Long,
        val apkVersionCode: Long,
    ) : InstallFailure

    /** Not enough storage, or the file could not be written. */
    data object Storage : InstallFailure

    /** The user cancelled the installation. */
    data object Cancelled : InstallFailure

    /**
     * Android's developer verification rejected the app: its developer is not registered, or
     * verification needs a network connection ([networkUnavailable]).
     */
    data class DeveloperVerificationFailed(
        val networkUnavailable: Boolean,
    ) : InstallFailure

    /** The system refused the installation, e.g. because of a device policy. */
    data class Blocked(
        val message: String?,
    ) : InstallFailure

    /** The APK does not run on this device (Android version, ABI, features). */
    data class Incompatible(
        val message: String?,
    ) : InstallFailure

    /** Any other error reported by the system installer. */
    data class SystemError(
        val message: String?,
    ) : InstallFailure
}
