package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus
import me.geckotv.apkmanager.feature.apps.AppIcon
import me.geckotv.apkmanager.feature.apps.AppItem
import me.geckotv.apkmanager.feature.apps.HeroIconSize
import me.geckotv.apkmanager.feature.apps.InfoLabel
import me.geckotv.apkmanager.feature.apps.InstallActions
import me.geckotv.apkmanager.feature.apps.R
import me.geckotv.apkmanager.feature.apps.hasSkippedLatestRelease

/**
 * The hero of the details screen: the app's icon, what its install status is, the versions, and the
 * button to install or update the app or the progress of the install. It is tinted while an update is
 * available.
 */
@Composable
internal fun AppDetailsHeader(
    item: AppItem,
    onInstallClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSkipClick: () -> Unit,
    onUndoSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgressIndicator: Boolean = true,
) {
    val app = item.app
    val hasUpdate = app.updateStatus == UpdateStatus.UpdateAvailable
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color =
            if (hasUpdate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor =
            if (hasUpdate) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    packageName = app.installedApp?.packageName,
                    avatarUrl = app.ownerAvatarUrl,
                    size = HeroIconSize,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.statusHeadline(),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        text = app.versionsText(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    HeaderLabels(isPrerelease = app.latestRelease?.isPrerelease == true, isPrivate = app.isPrivate)
                }
            }
            InstallActions(
                item = item,
                onInstallClick = onInstallClick,
                onCancelClick = onCancelClick,
                showProgressIndicator = showProgressIndicator,
            )
            val latestTag = app.latestRelease?.tagName
            if (latestTag != null && item.install == null) {
                when {
                    hasUpdate -> SkipAction(tag = latestTag, onClick = onSkipClick)
                    app.hasSkippedLatestRelease -> SkippedNotice(tag = latestTag, onUndoClick = onUndoSkipClick)
                }
            }
        }
    }
}

/** The button to skip the update to release [tag]. */
@Composable
private fun ColumnScope.SkipAction(
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.align(Alignment.CenterHorizontally),
        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
    ) {
        Text(stringResource(R.string.feature_apps_details_skip, tag))
    }
}

/** Says that release [tag] is skipped, and offers to stop skipping it. */
@Composable
private fun SkippedNotice(
    tag: String,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.feature_apps_status_skipped, tag),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onUndoClick) { Text(stringResource(R.string.feature_apps_details_skip_undo)) }
    }
}

@Composable
private fun HeaderLabels(
    isPrerelease: Boolean,
    isPrivate: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isPrerelease && !isPrivate) return
    Row(modifier = modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isPrerelease) InfoLabel(text = stringResource(R.string.feature_apps_pre_release))
        if (isPrivate) {
            InfoLabel(text = stringResource(R.string.feature_apps_private), icon = ApkManagerIcons.Lock)
        }
    }
}

/** What the user can do with the app next, in a few words. */
@Composable
private fun AppItem.statusHeadline(): String =
    stringResource(
        when (app.updateStatus) {
            UpdateStatus.UpdateAvailable -> {
                R.string.feature_apps_details_status_update_available
            }

            UpdateStatus.UpToDate -> {
                R.string.feature_apps_details_status_up_to_date
            }

            UpdateStatus.Unknown -> {
                R.string.feature_apps_details_status_unknown
            }

            UpdateStatus.NoRelease -> {
                R.string.feature_apps_details_status_no_release
            }

            UpdateStatus.NotInstalled -> {
                when {
                    app.latestRelease == null -> R.string.feature_apps_details_status_no_release
                    hasCompatibleApk -> R.string.feature_apps_details_status_ready
                    else -> R.string.feature_apps_details_status_no_apk
                }
            }
        },
    )

/** The installed version and the latest release, e.g. "Installed 1.1.0 · Latest v1.2.0". */
@Composable
private fun TrackedApp.versionsText(): String {
    val installed = installedApp
    val installedText =
        if (installed == null) {
            stringResource(R.string.feature_apps_details_not_installed)
        } else {
            stringResource(
                R.string.feature_apps_details_installed_version,
                installed.versionName ?: stringResource(R.string.feature_apps_unknown_version),
            )
        }
    val latestText = latestRelease?.let { stringResource(R.string.feature_apps_details_latest_release, it.tagName) }
    return listOfNotNull(installedText, latestText).joinToString(separator = " · ")
}
