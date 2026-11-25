package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.BackButton
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.component.section
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.InstalledApp

/**
 * Lets the user pick the installed app that belongs to a tracked app: a search field, the installed
 * apps and, first, "Not installed yet" to unlink.
 *
 * @param selectedPackageName the linked app, `null` if there is none.
 * @param onSelect called with the package name of the picked app, or `null` for "Not installed yet".
 */
@Composable
internal fun PackagePicker(
    installedApps: List<InstalledApp>,
    selectedPackageName: String?,
    onSelect: (String?) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var includeSystemApps by rememberSaveable { mutableStateOf(false) }
    val apps =
        remember(
            installedApps,
            query,
            includeSystemApps,
        ) { filterInstalledApps(installedApps, query, includeSystemApps) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBackClick)
            Text(
                text = stringResource(R.string.feature_apps_picker_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_apps_picker_search)) },
            leadingIcon = { Icon(painter = painterResource(ApkManagerIcons.Search), contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        Section {
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_apps_picker_show_system),
                    checked = includeSystemApps,
                    onCheckedChange = { includeSystemApps = it },
                )
            }
        }
        // `null` stands for "Not installed yet" and always comes first.
        LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
            section(items = listOf<InstalledApp?>(null) + apps, key = { it?.packageName.orEmpty() }) { app ->
                PackagePickerItem(
                    app = app,
                    isSelected = app?.packageName == selectedPackageName,
                    onClick = { onSelect(app?.packageName) },
                )
            }
            if (apps.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.feature_apps_picker_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PackagePickerItem(
    app: InstalledApp?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionItem(
        headline = app?.label ?: stringResource(R.string.feature_apps_picker_not_installed),
        modifier = modifier,
        supportingText = app?.packageName ?: stringResource(R.string.feature_apps_picker_not_installed_summary),
        leadingContent = {
            if (app == null) {
                SectionItemIcon(ApkManagerIcons.Close)
            } else {
                InstalledAppIcon(packageName = app.packageName) { SectionItemIcon(ApkManagerIcons.Apps) }
            }
        },
        trailingContent =
            if (isSelected) {
                { Icon(painter = painterResource(ApkManagerIcons.Check), contentDescription = null) }
            } else {
                null
            },
        onClick = onClick,
    )
}
