package me.geckotv.apkmanager.feature.apps

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.ApkAssetCandidate
import me.geckotv.apkmanager.core.model.RepositoryPreview
import java.text.NumberFormat

/** Review of a looked up repository: its release, the APK, the installed app, and the button to add it. */
@Composable
internal fun AddAppPreview(
    step: AddAppStep.Preview,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onAssetSelect: (String) -> Unit,
    onInstalledAppClick: () -> Unit,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            RepositoryHeader(preview = step.preview)
            ReleaseSection(step = step, onIncludePrereleasesChange = onIncludePrereleasesChange)
            if (step.release != null) {
                ApkSection(step = step, onAssetSelect = onAssetSelect)
            }
            InstalledAppSection(step = step, onClick = onInstalledAppClick)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBackClick) { Text(stringResource(R.string.feature_apps_add_back)) }
            Button(onClick = onAddClick, modifier = Modifier.weight(1f), enabled = !step.isAdding) {
                Text(stringResource(R.string.feature_apps_add_confirm))
            }
        }
    }
}

@Composable
private fun RepositoryHeader(
    preview: RepositoryPreview,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OwnerAvatar(avatarUrl = preview.ownerAvatarUrl, size = HeaderIconSize)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = preview.repository.fullName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            preview.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview.isPrivate) {
                    InfoLabel(text = stringResource(R.string.feature_apps_private), icon = ApkManagerIcons.Lock)
                }
                if (preview.isArchived) {
                    InfoLabel(text = stringResource(R.string.feature_apps_archived))
                }
                InfoLabel(text = NumberFormat.getIntegerInstance().format(preview.stars), icon = ApkManagerIcons.Star)
            }
        }
    }
}

@Composable
private fun ReleaseSection(
    step: AddAppStep.Preview,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val release = step.release
    Section(modifier = modifier, title = stringResource(R.string.feature_apps_release_section)) {
        item {
            SectionItem(
                headline = release?.tagName ?: stringResource(R.string.feature_apps_release_none),
                supportingText =
                    release?.publishedAt?.let {
                        stringResource(R.string.feature_apps_release_published, formatDate(it))
                    },
                leadingContent = { SectionItemIcon(ApkManagerIcons.NewReleases) },
                trailingContent =
                    if (release?.isPrerelease == true) {
                        { InfoLabel(text = stringResource(R.string.feature_apps_pre_release)) }
                    } else {
                        null
                    },
            )
        }
        item {
            SwitchSectionItem(
                headline = stringResource(R.string.feature_apps_include_prereleases),
                checked = step.includePrereleases,
                onCheckedChange = onIncludePrereleasesChange,
                supportingText = stringResource(R.string.feature_apps_include_prereleases_summary),
            )
        }
    }
}

@Composable
private fun ApkSection(
    step: AddAppStep.Preview,
    onAssetSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCompatibleApk = step.candidates.any { it.isCompatible }
    Section(modifier = modifier.selectableGroup(), title = stringResource(R.string.feature_apps_apk_section)) {
        if (!hasCompatibleApk) {
            item {
                SectionItem(
                    headline = stringResource(R.string.feature_apps_apk_none),
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Warning) },
                )
            }
        }
        step.candidates.forEach { candidate ->
            item {
                ApkItem(
                    candidate = candidate,
                    isSelectable = step.hasAssetChoice && candidate.isCompatible,
                    isSelected = candidate.asset == step.selectedAsset,
                    onClick = { onAssetSelect(candidate.asset.name) },
                )
            }
        }
    }
}

@Composable
private fun ApkItem(
    candidate: ApkAssetCandidate,
    isSelectable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val abi = candidate.abi ?: stringResource(R.string.feature_apps_apk_universal)
    val details =
        stringResource(
            R.string.feature_apps_apk_details,
            if (candidate.isCompatible) {
                Formatter.formatShortFileSize(LocalContext.current, candidate.asset.size)
            } else {
                stringResource(R.string.feature_apps_apk_not_compatible)
            },
            abi,
        )
    SectionItem(
        headline = candidate.asset.name,
        modifier =
            if (isSelectable) {
                modifier.semantics {
                    role = Role.RadioButton
                    selected = isSelected
                }
            } else {
                modifier
            },
        supportingText = details,
        enabled = candidate.isCompatible,
        leadingContent = { SectionItemIcon(ApkManagerIcons.Download) },
        trailingContent =
            if (isSelectable) {
                { RadioButton(selected = isSelected, onClick = null) }
            } else {
                null
            },
        onClick = if (isSelectable) onClick else null,
    )
}

@Composable
private fun InstalledAppSection(
    step: AddAppStep.Preview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = step.installedApp
    Section(modifier = modifier, title = stringResource(R.string.feature_apps_installed_section)) {
        item {
            SectionItem(
                headline = app?.label ?: stringResource(R.string.feature_apps_installed_none),
                supportingText = app?.packageName ?: stringResource(R.string.feature_apps_installed_choose),
                leadingContent = {
                    if (app == null) {
                        SectionItemIcon(ApkManagerIcons.Apps)
                    } else {
                        InstalledAppIcon(packageName = app.packageName) { SectionItemIcon(ApkManagerIcons.Apps) }
                    }
                },
                trailingContent = {
                    Icon(painter = painterResource(ApkManagerIcons.ChevronRight), contentDescription = null)
                },
                onClick = onClick,
            )
        }
    }
}
