package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.ApkAssets
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.toRegexOrNull
import me.geckotv.apkmanager.feature.apps.R

private const val EXCLUDED_ALPHA = 0.5f

/** How an APK of the latest release fares with the filter that is being edited. */
internal enum class AssetPreviewState {
    /** The APK that would be installed. */
    Selected,

    /** Passes the filter and fits this device, but another APK is preferred. */
    Compatible,

    /** Passes the filter but is built for another ABI. */
    Incompatible,

    /** The filter leaves the APK out. */
    Excluded,
}

/** An APK file of a release and what the filter does with it. */
internal data class AssetPreviewRow(
    val name: String,
    val state: AssetPreviewState,
)

/**
 * The `.apk` files of [release] and what [filter] does with each: the ones that pass the filter come
 * first, best first, then the ones it leaves out. [supportedAbis] and [preferredAssetName] decide which
 * APK would be installed.
 */
internal fun assetPreview(
    release: Release,
    filter: AssetFilter,
    supportedAbis: List<String>,
    preferredAssetName: String?,
): List<AssetPreviewRow> {
    val selected = ApkAssets.select(release.assets, supportedAbis, preferredAssetName, filter)
    val passing = ApkAssets.candidates(release.assets, supportedAbis, filter)
    val passingRows =
        passing.map { candidate ->
            val state =
                when {
                    candidate.asset.id == selected?.id -> AssetPreviewState.Selected
                    candidate.isCompatible -> AssetPreviewState.Compatible
                    else -> AssetPreviewState.Incompatible
                }
            AssetPreviewRow(candidate.asset.name, state)
        }
    val passingIds = passing.mapTo(mutableSetOf()) { it.asset.id }
    val excludedRows =
        ApkAssets
            .candidates(release.assets, supportedAbis)
            .filter { it.asset.id !in passingIds }
            .map { AssetPreviewRow(it.asset.name, AssetPreviewState.Excluded) }
    return passingRows + excludedRows
}

/**
 * The content of the sheet to edit the APK filter: two regular expressions for the file name and a
 * live preview of what they do to the APKs of the [release].
 *
 * @param filter the saved filter, which the fields start with.
 * @param release the latest release whose APKs are previewed, `null` if there is none.
 * @param supportedAbis the ABIs of this device.
 * @param preferredAssetName the asset the user picked earlier, see [ApkAssets.select].
 * @param onSaveClick called with the entered patterns; only possible while both are valid.
 */
@Composable
internal fun AssetFilterSheetContent(
    filter: AssetFilter,
    release: Release?,
    supportedAbis: List<String>,
    preferredAssetName: String?,
    onSaveClick: (include: String, exclude: String) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var include by rememberSaveable { mutableStateOf(filter.include.orEmpty()) }
    var exclude by rememberSaveable { mutableStateOf(filter.exclude.orEmpty()) }
    val isIncludeInvalid = include.isInvalidPattern()
    val isExcludeInvalid = exclude.isInvalidPattern()
    val draft = remember(include, exclude) { AssetFilter(include.ifBlank { null }, exclude.ifBlank { null }) }
    val preview =
        remember(release, draft) { release?.let { assetPreview(it, draft, supportedAbis, preferredAssetName) } }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SheetTitle(text = stringResource(R.string.feature_apps_filter_title))
        PatternField(
            value = include,
            onValueChange = { include = it },
            label = stringResource(R.string.feature_apps_filter_include_label),
            isError = isIncludeInvalid,
            imeAction = ImeAction.Next,
        )
        PatternField(
            value = exclude,
            onValueChange = { exclude = it },
            label = stringResource(R.string.feature_apps_filter_exclude_label),
            isError = isExcludeInvalid,
            imeAction = ImeAction.Done,
        )
        AssetPreviewList(release = release, rows = preview, hasFilter = !draft.isEmpty)
        OptionsSheetButtons(
            canReset = include.isNotEmpty() || exclude.isNotEmpty(),
            canSave = !isIncludeInvalid && !isExcludeInvalid,
            onResetClick = {
                include = ""
                exclude = ""
            },
            onCancelClick = onCancelClick,
            onSaveClick = { onSaveClick(include, exclude) },
        )
    }
}

@Composable
private fun AssetPreviewList(
    release: Release?,
    rows: List<AssetPreviewRow>?,
    hasFilter: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (release != null) {
            Text(
                text = stringResource(R.string.feature_apps_filter_preview, release.tagName),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    rows == null -> PreviewMessage(stringResource(R.string.feature_apps_filter_preview_no_release))
                    rows.isEmpty() -> PreviewMessage(stringResource(R.string.feature_apps_filter_preview_none))
                    else -> rows.forEach { AssetPreviewItem(it) }
                }
                if (hasFilter && rows != null && rows.none { it.state == AssetPreviewState.Selected }) {
                    PreviewMessage(stringResource(R.string.feature_apps_filter_no_match), isError = true)
                }
            }
        }
    }
}

@Composable
private fun PreviewMessage(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier.padding(8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AssetPreviewItem(
    row: AssetPreviewRow,
    modifier: Modifier = Modifier,
) {
    val isSelected = row.state == AssetPreviewState.Selected
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (row.state == AssetPreviewState.Excluded) EXCLUDED_ALPHA else 1f),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(row.state.icon()),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.name, style = MaterialTheme.typography.bodyMedium)
                row.state.label()?.let { label -> Text(text = label, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

private fun AssetPreviewState.icon(): Int =
    when (this) {
        AssetPreviewState.Selected, AssetPreviewState.Compatible -> ApkManagerIcons.Check
        AssetPreviewState.Incompatible -> ApkManagerIcons.Warning
        AssetPreviewState.Excluded -> ApkManagerIcons.Close
    }

@Composable
private fun AssetPreviewState.label(): String? =
    when (this) {
        AssetPreviewState.Selected -> stringResource(R.string.feature_apps_filter_will_install)
        AssetPreviewState.Compatible -> null
        AssetPreviewState.Incompatible -> stringResource(R.string.feature_apps_filter_incompatible)
        AssetPreviewState.Excluded -> stringResource(R.string.feature_apps_filter_excluded)
    }
