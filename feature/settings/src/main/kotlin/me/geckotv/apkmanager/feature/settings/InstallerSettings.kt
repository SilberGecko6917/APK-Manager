package me.geckotv.apkmanager.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.InstallerBackend

/** Adds the "Installer" section to a `LazyColumn`. */
internal fun LazyListScope.installerSettings(
    uiState: SettingsUiState.Success,
    actions: InstallerActions,
) {
    item(key = "installer") {
        InstallerSettings(
            selected = uiState.settings.installerBackend,
            shizukuState = uiState.shizukuState,
            actions = actions,
        )
    }
}

/** What the user can do in the "Installer" section. */
internal data class InstallerActions(
    val onBackendSelect: (InstallerBackend) -> Unit = {},
    val onRequestPermission: () -> Unit = {},
    val onOpenShizukuApp: () -> Unit = {},
    val onOpenShizukuWebsite: () -> Unit = {},
)

/**
 * The "Installer" section: Android's package installer or Shizuku. While Shizuku is selected, a status
 * row says whether it can be used and offers the step that is missing.
 */
@Composable
private fun InstallerSettings(
    selected: InstallerBackend,
    shizukuState: ShizukuState,
    actions: InstallerActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Section(title = stringResource(R.string.feature_settings_installer)) {
            item {
                BackendOption(
                    headline = stringResource(R.string.feature_settings_installer_android),
                    supportingText = stringResource(R.string.feature_settings_installer_android_summary),
                    isSelected = selected == InstallerBackend.System,
                    onClick = { actions.onBackendSelect(InstallerBackend.System) },
                )
            }
            item {
                BackendOption(
                    headline = stringResource(R.string.feature_settings_installer_shizuku),
                    supportingText = stringResource(R.string.feature_settings_installer_shizuku_summary),
                    isSelected = selected == InstallerBackend.Shizuku,
                    onClick = { actions.onBackendSelect(InstallerBackend.Shizuku) },
                )
            }
            if (selected == InstallerBackend.Shizuku) {
                item { ShizukuStatusItem(state = shizukuState, actions = actions) }
            }
        }
        Text(
            text = stringResource(R.string.feature_settings_installer_info),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackendOption(
    headline: String,
    supportingText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    SectionItem(
        headline = headline,
        supportingText = supportingText,
        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
        onClick = onClick,
    )
}

/** Whether Shizuku can be used, with a button for the step that is missing. */
@Composable
private fun ShizukuStatusItem(
    state: ShizukuState,
    actions: InstallerActions,
    modifier: Modifier = Modifier,
) {
    val status = state.status(actions)
    SectionItem(
        headline = status.headline,
        modifier = modifier,
        supportingText = status.supportingText,
        leadingContent = {
            SectionItemIcon(if (state is ShizukuState.Ready) ApkManagerIcons.CheckCircle else ApkManagerIcons.Warning)
        },
        trailingContent =
            status.action?.let { action ->
                { TextButton(onClick = action.onClick) { Text(action.label) } }
            },
    )
}

private class ShizukuStatus(
    val headline: String,
    val supportingText: String,
    val action: StatusAction? = null,
)

private class StatusAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun status(
    @StringRes headline: Int,
    @StringRes supportingText: Int,
    @StringRes actionLabel: Int? = null,
    onAction: () -> Unit = {},
): ShizukuStatus =
    ShizukuStatus(
        headline = stringResource(headline),
        supportingText = stringResource(supportingText),
        action = actionLabel?.let { StatusAction(stringResource(it), onAction) },
    )

@Composable
private fun ShizukuState.status(actions: InstallerActions): ShizukuStatus =
    when (this) {
        ShizukuState.NotInstalled -> {
            status(
                R.string.feature_settings_shizuku_not_installed,
                R.string.feature_settings_shizuku_not_installed_summary,
                R.string.feature_settings_shizuku_get,
                actions.onOpenShizukuWebsite,
            )
        }

        ShizukuState.NotRunning -> {
            status(
                R.string.feature_settings_shizuku_not_running,
                R.string.feature_settings_shizuku_not_running_summary,
                R.string.feature_settings_shizuku_open,
                actions.onOpenShizukuApp,
            )
        }

        ShizukuState.Unsupported -> {
            status(
                R.string.feature_settings_shizuku_unsupported,
                R.string.feature_settings_shizuku_unsupported_summary,
                R.string.feature_settings_shizuku_get,
                actions.onOpenShizukuWebsite,
            )
        }

        ShizukuState.PermissionRequired -> {
            status(
                R.string.feature_settings_shizuku_permission_required,
                R.string.feature_settings_shizuku_permission_required_summary,
                R.string.feature_settings_shizuku_allow,
                actions.onRequestPermission,
            )
        }

        ShizukuState.PermissionDenied -> {
            status(
                R.string.feature_settings_shizuku_permission_denied,
                R.string.feature_settings_shizuku_permission_denied_summary,
                R.string.feature_settings_shizuku_open,
                actions.onOpenShizukuApp,
            )
        }

        is ShizukuState.Ready -> {
            val summary =
                if (isRoot) {
                    R.string.feature_settings_shizuku_ready_root
                } else {
                    R.string.feature_settings_shizuku_ready_adb
                }
            status(R.string.feature_settings_shizuku_ready, summary)
        }
    }
