package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.installer.InstallState

private val InstallButtonHeight = 56.dp

/**
 * The button to install or update the app, or the progress of the install that is running with the
 * buttons to cancel it or to show its confirmation again.
 *
 * @param showProgressIndicator whether to show indicators of unknown progress; they animate forever,
 * which screenshot tests cannot wait for.
 */
@Composable
internal fun InstallActions(
    item: AppItem,
    onInstallClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgressIndicator: Boolean = true,
) {
    val install = item.install
    val tag = item.app.latestRelease?.tagName
    val installAction = item.installAction
    if (install != null) {
        InstallProgress(
            state = install,
            onConfirmClick = onInstallClick.takeIf { installAction == InstallAction.Confirm },
            onCancelClick = onCancelClick,
            showProgressIndicator = showProgressIndicator,
            modifier = modifier,
        )
    } else if (installAction != null && tag != null) {
        val label =
            when (installAction) {
                InstallAction.Install -> stringResource(R.string.feature_apps_details_install, tag)
                InstallAction.Update -> stringResource(R.string.feature_apps_details_update, tag)
                InstallAction.Confirm -> stringResource(R.string.feature_apps_install_confirm)
            }
        Button(
            onClick = onInstallClick,
            modifier = modifier.fillMaxWidth().heightIn(min = InstallButtonHeight),
        ) {
            Icon(
                painter = painterResource(ApkManagerIcons.Download),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(text = label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InstallProgress(
    state: InstallState,
    onConfirmClick: (() -> Unit)?,
    onCancelClick: () -> Unit,
    showProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = (state as? InstallState.Downloading)?.progress
    Column(modifier = modifier.fillMaxWidth()) {
        if (progress != null) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        } else if (showProgressIndicator) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.progressText(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCancelClick) { Text(stringResource(R.string.feature_apps_details_cancel)) }
            if (onConfirmClick != null) {
                Button(onClick = onConfirmClick) { Text(stringResource(R.string.feature_apps_install_confirm)) }
            }
        }
    }
}
