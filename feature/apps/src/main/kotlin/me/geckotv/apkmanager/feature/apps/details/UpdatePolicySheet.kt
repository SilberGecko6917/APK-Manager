package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.model.UpdatePolicy
import me.geckotv.apkmanager.feature.apps.R

/**
 * The content of the sheet to choose what happens when an update of the app is found.
 *
 * @param selected the app's own policy, `null` if it follows the [defaultPolicy] of the settings.
 * @param confirmHint why Android asks to confirm automatic updates of this app, if it does.
 * @param onSelect called with the chosen policy, `null` for the default; the sheet closes then.
 */
@Composable
internal fun UpdatePolicySheetContent(
    selected: UpdatePolicy?,
    defaultPolicy: UpdatePolicy,
    confirmHint: UpdateConfirmHint?,
    onSelect: (UpdatePolicy?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SheetTitle(text = stringResource(R.string.feature_apps_updates_title))
        Section(modifier = Modifier.padding(bottom = 8.dp)) {
            item {
                PolicyOption(
                    headline = stringResource(R.string.feature_apps_updates_use_default),
                    supportingText =
                        stringResource(R.string.feature_apps_updates_use_default_summary, defaultPolicy.label()),
                    isSelected = selected == null,
                    onClick = { onSelect(null) },
                )
            }
            item {
                PolicyOption(
                    headline = UpdatePolicy.Notify.label(),
                    supportingText = stringResource(R.string.feature_apps_updates_notify_summary),
                    isSelected = selected == UpdatePolicy.Notify,
                    onClick = { onSelect(UpdatePolicy.Notify) },
                )
            }
            item {
                PolicyOption(
                    headline = UpdatePolicy.Install.label(),
                    supportingText =
                        listOfNotNull(
                            stringResource(R.string.feature_apps_updates_install_summary),
                            confirmHint?.text(),
                        ).joinToString(separator = "\n"),
                    isSelected = selected == UpdatePolicy.Install,
                    onClick = { onSelect(UpdatePolicy.Install) },
                )
            }
        }
    }
}

@Composable
private fun PolicyOption(
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

/** What the "Updates" row says: the own [policy] or the [default] it follows, plus the [hint] if it installs. */
@Composable
internal fun updatePolicySummary(
    policy: UpdatePolicy?,
    default: UpdatePolicy,
    hint: UpdateConfirmHint?,
): String {
    val effective = policy ?: default
    val label =
        if (policy == null) {
            stringResource(R.string.feature_apps_updates_default, effective.label())
        } else {
            effective.label()
        }
    val confirmText = hint?.takeIf { effective == UpdatePolicy.Install }?.text()
    return listOfNotNull(label, confirmText).joinToString(separator = "\n")
}

@Composable
private fun UpdatePolicy.label(): String =
    when (this) {
        UpdatePolicy.Notify -> stringResource(R.string.feature_apps_updates_notify)
        UpdatePolicy.Install -> stringResource(R.string.feature_apps_updates_install)
    }

@Composable
private fun UpdateConfirmHint.text(): String =
    when (this) {
        UpdateConfirmHint.AnotherInstaller -> stringResource(R.string.feature_apps_updates_confirm_installer)
        UpdateConfirmHint.OldTargetSdk -> stringResource(R.string.feature_apps_updates_confirm_target_sdk)
        UpdateConfirmHint.OldAndroid -> stringResource(R.string.feature_apps_updates_confirm_android)
    }
