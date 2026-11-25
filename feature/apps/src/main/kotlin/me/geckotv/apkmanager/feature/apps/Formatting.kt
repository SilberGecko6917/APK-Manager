package me.geckotv.apkmanager.feature.apps

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
    }

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
