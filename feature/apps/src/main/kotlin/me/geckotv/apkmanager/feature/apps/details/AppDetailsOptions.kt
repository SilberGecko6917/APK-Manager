package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerBottomSheet
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.section
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.feature.apps.R

/** What the user can do besides installing the app: skip a release and edit the advanced options. */
internal data class AppDetailsOptionsActions(
    val onSkipClick: () -> Unit = {},
    val onUndoSkipClick: () -> Unit = {},
    val onAssetFilterClick: () -> Unit = {},
    val onAssetFilterSave: (include: String, exclude: String) -> Unit = { _, _ -> },
    val onVersionPatternClick: () -> Unit = {},
    val onVersionPatternSave: (String) -> Unit = {},
    val onDismiss: () -> Unit = {},
)

/** The "Advanced" section: the options that change how releases and their APKs are read. */
internal fun LazyListScope.advancedSection(
    title: String,
    app: TrackedApp,
    options: AppDetailsOptionsActions,
) {
    section(items = AdvancedRow.entries, title = title, key = { it }) { row ->
        AdvancedItem(row = row, app = app, options = options)
    }
}

private enum class AdvancedRow {
    AssetFilter,
    VersionPattern,
}

@Composable
private fun AdvancedItem(
    row: AdvancedRow,
    app: TrackedApp,
    options: AppDetailsOptionsActions,
) {
    when (row) {
        AdvancedRow.AssetFilter -> {
            SectionItem(
                headline = stringResource(R.string.feature_apps_filter_title),
                supportingText = app.assetFilter.summary(),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Search) },
                trailingContent = {
                    Icon(painter = painterResource(ApkManagerIcons.ChevronRight), contentDescription = null)
                },
                onClick = options.onAssetFilterClick,
            )
        }

        AdvancedRow.VersionPattern -> {
            SectionItem(
                headline = stringResource(R.string.feature_apps_pattern_title),
                supportingText = app.versionPattern ?: stringResource(R.string.feature_apps_pattern_automatic),
                leadingContent = { SectionItemIcon(ApkManagerIcons.NewReleases) },
                trailingContent = {
                    Icon(painter = painterResource(ApkManagerIcons.ChevronRight), contentDescription = null)
                },
                onClick = options.onVersionPatternClick,
            )
        }
    }
}

/** What the filter does in a few words, e.g. "Contains foss · Without debug". */
@Composable
private fun AssetFilter.summary(): String =
    if (isEmpty) {
        stringResource(R.string.feature_apps_filter_all)
    } else {
        listOfNotNull(
            include?.takeIf(String::isNotBlank)?.let { stringResource(R.string.feature_apps_filter_contains, it) },
            exclude?.takeIf(String::isNotBlank)?.let { stringResource(R.string.feature_apps_filter_without, it) },
        ).joinToString(separator = " · ")
    }

/** The sheet to edit the advanced option that is shown in [state], if any. */
@Composable
internal fun OptionsSheets(
    state: AppDetailsUiState.Ready,
    options: AppDetailsOptionsActions,
) {
    val app = state.item.app
    state.optionsSheet?.let { sheet ->
        ApkManagerBottomSheet(onDismissRequest = options.onDismiss) {
            when (sheet) {
                is OptionsSheet.AssetFilterSheet -> {
                    AssetFilterSheetContent(
                        filter = app.assetFilter,
                        release = app.latestRelease,
                        supportedAbis = sheet.supportedAbis,
                        preferredAssetName = app.preferredAssetName,
                        onSaveClick = options.onAssetFilterSave,
                        onCancelClick = options.onDismiss,
                    )
                }

                OptionsSheet.VersionPatternSheet -> {
                    VersionPatternSheetContent(
                        pattern = app.versionPattern,
                        tags =
                            previewTags(
                                latestRelease = app.latestRelease,
                                history = (state.history as? ReleaseHistoryState.Loaded)?.releases.orEmpty(),
                            ),
                        onSaveClick = options.onVersionPatternSave,
                        onCancelClick = options.onDismiss,
                    )
                }
            }
        }
    }
}
