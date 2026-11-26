package me.geckotv.apkmanager.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.core.model.UserSettings

/** Adds the "Automatic updates" section to a `LazyColumn`. */
internal fun LazyListScope.automaticUpdates(
    settings: UserSettings,
    actions: AutomaticUpdatesActions,
) {
    item(key = "automatic-updates") { AutomaticUpdateSettings(settings = settings, actions = actions) }
}

/** What the user can change about automatic updates. */
internal data class AutomaticUpdatesActions(
    val onInstallAutomaticallyChange: (Boolean) -> Unit = {},
    val onUnmeteredOnlyChange: (Boolean) -> Unit = {},
    val onChargingOnlyChange: (Boolean) -> Unit = {},
)

/**
 * The "Automatic updates" section. Automatic updates run as part of the background checks, so the
 * whole section is disabled while those are off. The conditions stay enabled while the default is
 * "notify only", because apps can still choose to install automatically.
 */
@Composable
private fun AutomaticUpdateSettings(
    settings: UserSettings,
    actions: AutomaticUpdatesActions,
    modifier: Modifier = Modifier,
) {
    val enabled = settings.backgroundChecksEnabled
    Column(modifier = modifier) {
        Section(title = stringResource(R.string.feature_settings_automatic_updates)) {
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_settings_install_automatically),
                    supportingText = stringResource(R.string.feature_settings_install_automatically_summary),
                    checked = settings.defaultUpdatePolicy == UpdatePolicy.Install,
                    onCheckedChange = actions.onInstallAutomaticallyChange,
                    enabled = enabled,
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Download) },
                )
            }
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_settings_unmetered_only),
                    supportingText = stringResource(R.string.feature_settings_auto_update_unmetered_summary),
                    checked = settings.autoUpdateOnlyOnUnmeteredNetwork,
                    onCheckedChange = actions.onUnmeteredOnlyChange,
                    enabled = enabled,
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Wifi) },
                )
            }
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_settings_charging_only),
                    supportingText = stringResource(R.string.feature_settings_charging_only_summary),
                    checked = settings.autoUpdateOnlyWhileCharging,
                    onCheckedChange = actions.onChargingOnlyChange,
                    enabled = enabled,
                    leadingContent = { SectionItemIcon(ApkManagerIcons.BatteryChargingFull) },
                )
            }
        }
        Text(
            text = stringResource(R.string.feature_settings_automatic_updates_info),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
