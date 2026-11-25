package me.geckotv.apkmanager.feature.apps

import android.content.res.Resources
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.SourceError
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** A date in the user's language, e.g. "Nov 20, 2026". */
internal fun formatDate(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zone).format(instant)

/** A time of day in the user's language, e.g. "14:05". */
internal fun formatTime(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)

/** The text of a snackbar about an update check. */
internal fun AppsMessage.text(resources: Resources): String =
    when (this) {
        is AppsMessage.CheckFailed -> {
            resources.getQuantityString(R.plurals.feature_apps_message_check_failed, count, count)
        }

        is AppsMessage.RateLimited -> {
            if (retryAt == null) {
                resources.getString(R.string.feature_apps_message_rate_limited)
            } else {
                resources.getString(R.string.feature_apps_message_rate_limited_at, formatTime(retryAt))
            }
        }

        AppsMessage.Offline -> {
            resources.getString(R.string.feature_apps_message_offline)
        }

        AppsMessage.Unauthorized -> {
            resources.getString(R.string.feature_apps_message_unauthorized)
        }

        is AppsMessage.Installed -> {
            resources.getString(R.string.feature_apps_message_installed, name)
        }

        is AppsMessage.Updated -> {
            if (version == null) {
                resources.getString(R.string.feature_apps_message_updated_unknown_version, name)
            } else {
                resources.getString(R.string.feature_apps_message_updated, name, version)
            }
        }

        is AppsMessage.InstallFailed -> {
            failure.text(resources, tag)
        }
    }

/** Whether the snackbar of this message offers to open the system setting that lets APK Manager install apps. */
internal val AppsMessage.offersInstallPermission: Boolean
    get() = this is AppsMessage.InstallFailed && failure == InstallFailure.PermissionMissing

/** Messages about a failed install are long, so they stay until the user dismisses them. */
internal val AppsMessage.isInstallFailure: Boolean
    get() = this is AppsMessage.InstallFailed

/** The text of a snackbar about the failed install of release [tag]. */
internal fun InstallFailure.text(
    resources: Resources,
    tag: String?,
): String =
    when (this) {
        InstallFailure.NoCompatibleApk -> {
            if (tag == null) {
                resources.getString(R.string.feature_apps_message_no_apk_latest)
            } else {
                resources.getString(R.string.feature_apps_message_no_apk, tag)
            }
        }

        is InstallFailure.DownloadFailed -> {
            error.downloadText(resources)
        }

        is InstallFailure.PackageMismatch -> {
            resources.getString(R.string.feature_apps_message_package_mismatch, expected, actual)
        }

        is InstallFailure.Blocked -> {
            installFailedText(resources, message)
        }

        is InstallFailure.Incompatible -> {
            installFailedText(resources, message)
        }

        is InstallFailure.SystemError -> {
            installFailedText(resources, message)
        }

        else -> {
            resources.getString(fixedMessageRes())
        }
    }

/** The message of failures that need no details. */
private fun InstallFailure.fixedMessageRes(): Int =
    when (this) {
        InstallFailure.PermissionMissing -> {
            R.string.feature_apps_message_permission_missing
        }

        InstallFailure.ChecksumMismatch -> {
            R.string.feature_apps_message_checksum_mismatch
        }

        InstallFailure.InvalidApk -> {
            R.string.feature_apps_message_invalid_apk
        }

        InstallFailure.SignatureMismatch -> {
            R.string.feature_apps_message_signature_mismatch
        }

        is InstallFailure.Downgrade -> {
            R.string.feature_apps_message_downgrade
        }

        InstallFailure.Storage -> {
            R.string.feature_apps_message_storage
        }

        is InstallFailure.DeveloperVerificationFailed -> {
            if (networkUnavailable) {
                R.string.feature_apps_message_developer_verification_offline
            } else {
                R.string.feature_apps_message_developer_verification_failed
            }
        }

        else -> {
            R.string.feature_apps_message_install_failed
        }
    }

private fun installFailedText(
    resources: Resources,
    message: String?,
): String =
    if (message.isNullOrBlank()) {
        resources.getString(R.string.feature_apps_message_install_failed)
    } else {
        resources.getString(R.string.feature_apps_message_install_failed_with, message)
    }

/** Why a download failed, in the wording of the update check. */
private fun SourceError.downloadText(resources: Resources): String =
    when (this) {
        SourceError.NotFound -> {
            resources.getString(R.string.feature_apps_message_download_not_found)
        }

        is SourceError.RateLimited -> {
            AppsMessage.RateLimited(retryAt).text(resources)
        }

        SourceError.Offline -> {
            resources.getString(R.string.feature_apps_message_offline)
        }

        SourceError.Unauthorized -> {
            resources.getString(R.string.feature_apps_message_unauthorized)
        }

        is SourceError.Forbidden, SourceError.Unavailable, is SourceError.Unexpected -> {
            resources.getString(R.string.feature_apps_message_download_failed)
        }
    }

/** What a running install is doing, e.g. "Downloading 45 %". */
@Composable
internal fun InstallState.progressText(): String =
    when (this) {
        is InstallState.Downloading -> {
            val progress = progress
            if (progress != null) {
                stringResource(R.string.feature_apps_install_downloading_percent, (progress * PERCENT).toInt())
            } else {
                stringResource(
                    R.string.feature_apps_install_downloading_size,
                    Formatter.formatShortFileSize(LocalContext.current, bytesRead),
                )
            }
        }

        InstallState.Verifying -> {
            stringResource(R.string.feature_apps_install_verifying)
        }

        InstallState.Installing -> {
            stringResource(R.string.feature_apps_install_installing)
        }

        is InstallState.WaitingForUser -> {
            stringResource(R.string.feature_apps_install_waiting)
        }

        is InstallState.Succeeded, is InstallState.Failed -> {
            ""
        }
    }

private const val PERCENT = 100

/** What to tell the user when a repository lookup failed: the problem, and a hint if there is one. */
@Composable
internal fun SourceError.lookUpMessages(): List<String> =
    when (this) {
        SourceError.NotFound -> {
            listOf(stringResource(R.string.feature_apps_error_not_found))
        }

        is SourceError.RateLimited -> {
            listOfNotNull(
                stringResource(R.string.feature_apps_error_rate_limited),
                retryAt?.let { stringResource(R.string.feature_apps_error_rate_limited_at, formatTime(it)) },
                stringResource(R.string.feature_apps_error_rate_limited_hint),
            )
        }

        SourceError.Offline -> {
            listOf(stringResource(R.string.feature_apps_error_offline))
        }

        SourceError.Unauthorized -> {
            listOf(stringResource(R.string.feature_apps_error_unauthorized))
        }

        is SourceError.Forbidden, SourceError.Unavailable, is SourceError.Unexpected -> {
            listOf(stringResource(R.string.feature_apps_error_generic))
        }
    }
