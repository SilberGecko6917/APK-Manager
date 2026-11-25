package me.geckotv.apkmanager.feature.apps

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus

/** A tracked app in the apps list. */
@Composable
internal fun AppListItem(
    app: TrackedApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionItem(
        headline = app.displayName,
        modifier = modifier,
        supportingText = app.statusText(),
        leadingContent = {
            AppIcon(
                packageName = app.installedApp?.packageName,
                avatarUrl = app.ownerAvatarUrl,
            )
        },
        trailingContent = {
            val isPrerelease = app.latestRelease?.isPrerelease == true
            if (isPrerelease || app.isPrivate) {
                Row(
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
                }
            }
        },
        onClick = onClick,
    )
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
