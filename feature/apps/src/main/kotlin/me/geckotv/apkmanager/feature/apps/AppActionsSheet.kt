package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerBottomSheet
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.TrackedApp

/** Bottom sheet with the actions for one tracked app, and the list of installed apps to link. */
@Composable
internal fun AppActionsSheet(
    state: AppActionsUiState,
    onDismiss: () -> Unit,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onInstalledAppClick: () -> Unit,
    onPackageSelect: (String?) -> Unit,
    onPickerBack: () -> Unit,
    onRemoveConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var isConfirmingRemove by rememberSaveable { mutableStateOf(false) }
    val installedApps = state.installedApps
    val app = state.app

    ApkManagerBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        when {
            installedApps != null -> {
                PackagePicker(
                    installedApps = installedApps,
                    selectedPackageName = app.packageName,
                    onSelect = onPackageSelect,
                    onBackClick = onPickerBack,
                )
            }

            isConfirmingRemove -> {
                RemoveConfirmation(
                    name = app.displayName,
                    onCancelClick = { isConfirmingRemove = false },
                    onRemoveClick = onRemoveConfirm,
                )
            }

            else -> {
                AppActionsContent(
                    app = app,
                    onOpenRepositoryClick = { uriHandler.openUri(app.repository.htmlUrl) },
                    onReleaseNotesClick = { app.latestRelease?.let { uriHandler.openUri(it.htmlUrl) } },
                    onIncludePrereleasesChange = onIncludePrereleasesChange,
                    onInstalledAppClick = onInstalledAppClick,
                    onRemoveClick = { isConfirmingRemove = true },
                )
            }
        }
    }
}

/** The header and the actions of a tracked app. */
@Composable
internal fun AppActionsContent(
    app: TrackedApp,
    onOpenRepositoryClick: () -> Unit,
    onReleaseNotesClick: () -> Unit,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onInstalledAppClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppActionsHeader(app = app)
        AppActionsSection(
            app = app,
            onOpenRepositoryClick = onOpenRepositoryClick,
            onReleaseNotesClick = onReleaseNotesClick,
            onIncludePrereleasesChange = onIncludePrereleasesChange,
            onInstalledAppClick = onInstalledAppClick,
            modifier = Modifier.padding(top = 8.dp),
        )
        FilledTonalButton(
            onClick = onRemoveClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
        ) {
            Icon(
                painter = painterResource(ApkManagerIcons.Delete),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(text = stringResource(R.string.feature_apps_actions_remove), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AppActionsSection(
    app: TrackedApp,
    onOpenRepositoryClick: () -> Unit,
    onReleaseNotesClick: () -> Unit,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onInstalledAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier) {
        item {
            SectionItem(
                headline = stringResource(R.string.feature_apps_actions_open_github),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Code) },
                trailingContent = {
                    Icon(
                        painter = painterResource(ApkManagerIcons.OpenInNew),
                        contentDescription = null,
                    )
                },
                onClick = onOpenRepositoryClick,
            )
        }
        if (app.latestRelease != null) {
            item {
                SectionItem(
                    headline = stringResource(R.string.feature_apps_actions_release_notes),
                    leadingContent = { SectionItemIcon(ApkManagerIcons.NewReleases) },
                    trailingContent = {
                        Icon(painter = painterResource(ApkManagerIcons.OpenInNew), contentDescription = null)
                    },
                    onClick = onReleaseNotesClick,
                )
            }
        }
        item {
            SwitchSectionItem(
                headline = stringResource(R.string.feature_apps_include_prereleases),
                checked = app.includePrereleases,
                onCheckedChange = onIncludePrereleasesChange,
                supportingText = stringResource(R.string.feature_apps_include_prereleases_summary),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Upgrade) },
            )
        }
        item {
            SectionItem(
                headline = stringResource(R.string.feature_apps_installed_section),
                supportingText =
                    app.installedApp?.let { "${it.label} · ${it.packageName}" }
                        ?: app.packageName
                        ?: stringResource(R.string.feature_apps_installed_none),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Apps) },
                trailingContent = {
                    Icon(painter = painterResource(ApkManagerIcons.ChevronRight), contentDescription = null)
                },
                onClick = onInstalledAppClick,
            )
        }
    }
}

@Composable
private fun AppActionsHeader(
    app: TrackedApp,
    modifier: Modifier = Modifier,
) {
    val installedVersion =
        app.installedApp?.let {
            it.versionName
                ?: stringResource(R.string.feature_apps_unknown_version)
        }
    val details =
        listOfNotNull(
            installedVersion?.let { stringResource(R.string.feature_apps_actions_installed_version, it) }
                ?: stringResource(R.string.feature_apps_actions_not_installed),
            app.latestRelease?.let { stringResource(R.string.feature_apps_actions_latest_release, it.tagName) },
        ).joinToString(separator = " · ")
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.installedApp?.packageName, avatarUrl = app.ownerAvatarUrl, size = HeaderIconSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.repository.fullName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemoveConfirmation(
    name: String,
    onCancelClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.feature_apps_actions_remove_title, name),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
            text = stringResource(R.string.feature_apps_actions_remove_message),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancelClick) { Text(stringResource(R.string.feature_apps_actions_cancel)) }
            Button(
                onClick = onRemoveClick,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.feature_apps_actions_remove))
            }
        }
    }
}
