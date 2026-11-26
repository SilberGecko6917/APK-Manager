package me.geckotv.apkmanager.feature.apps

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus

/**
 * A tracked app in the apps list.
 *
 * @param onInstallClick called when the user taps the "Install" or "Update" button.
 * @param showProgressIndicator whether to show indicators of unknown progress; they animate forever,
 * which screenshot tests cannot wait for.
 */
@Composable
internal fun AppListItem(
    item: AppItem,
    onClick: () -> Unit,
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgressIndicator: Boolean = true,
) {
    val app = item.app
    SectionItem(
        headline = app.displayName,
        modifier = modifier,
        supportingText = item.install?.progressText() ?: app.statusText(),
        leadingContent = {
            AppIcon(
                packageName = app.installedApp?.packageName,
                avatarUrl = app.ownerAvatarUrl,
            )
        },
        trailingContent = {
            AppListItemTrailing(
                item = item,
                onInstallClick = onInstallClick,
                showProgressIndicator = showProgressIndicator,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun AppListItemTrailing(
    item: AppItem,
    onInstallClick: () -> Unit,
    showProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    val app = item.app
    val isPrerelease = app.latestRelease?.isPrerelease == true
    val installAction = item.installAction
    val hasBadges = isPrerelease || app.isPrivate
    val hasInstallContent = installAction != null || item.install != null
    if (hasBadges || hasInstallContent) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPrerelease) InfoLabel(text = stringResource(R.string.feature_apps_pre_release))
            if (app.isPrivate) {
                Icon(
                    painter = painterResource(ApkManagerIcons.Lock),
                    contentDescription = stringResource(R.string.feature_apps_private),
                    modifier = Modifier.size(18.dp),
                )
            }
            if (item.install != null && installAction == null) {
                InstallProgressIndicator(
                    state = item.install,
                    showProgressIndicator = showProgressIndicator,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (installAction != null) {
                FilledTonalButton(onClick = onInstallClick, contentPadding = ButtonDefaults.SmallContentPadding) {
                    Text(text = stringResource(installAction.labelRes()))
                }
            }
        }
    }
}

/** A circle that fills as the download progresses; the other steps have no progress to show. */
@Composable
private fun InstallProgressIndicator(
    state: InstallState,
    showProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = (state as? InstallState.Downloading)?.progress
    if (progress != null) {
        CircularProgressIndicator(progress = { progress }, modifier = modifier, strokeWidth = 3.dp)
    } else if (showProgressIndicator) {
        CircularProgressIndicator(modifier = modifier, strokeWidth = 3.dp)
    }
}

internal fun InstallAction.labelRes(): Int =
    when (this) {
        InstallAction.Install -> R.string.feature_apps_install
        InstallAction.Update -> R.string.feature_apps_update
        InstallAction.Confirm -> R.string.feature_apps_install_confirm
    }

/** The text below an app's name: the versions that matter for its [UpdateStatus]. */
@Composable
private fun TrackedApp.statusText(): String {
    val tag = latestRelease?.tagName
    val installedVersion = installedApp?.versionName ?: stringResource(R.string.feature_apps_unknown_version)
    return when {
        tag == null && updateStatus == UpdateStatus.NotInstalled -> {
            repository.fullName
        }

        tag == null -> {
            stringResource(R.string.feature_apps_status_no_release)
        }

        updateStatus == UpdateStatus.UpdateAvailable -> {
            stringResource(R.string.feature_apps_status_update_available, installedVersion, tag)
        }

        updateStatus == UpdateStatus.Unknown -> {
            stringResource(R.string.feature_apps_status_unknown, installedVersion, tag)
        }

        updateStatus == UpdateStatus.NotInstalled -> {
            stringResource(R.string.feature_apps_status_not_installed, tag, repository.fullName)
        }

        else -> {
            tag
        }
    }
}

/** A small rounded label, e.g. "Pre-release". */
@Composable
internal fun InfoLabel(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
