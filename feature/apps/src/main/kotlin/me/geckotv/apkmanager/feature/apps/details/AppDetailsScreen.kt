package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerBottomSheet
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerTopAppBar
import me.geckotv.apkmanager.core.designsystem.component.BackButton
import me.geckotv.apkmanager.core.designsystem.component.SectionHeader
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.component.section
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.feature.apps.InfoLabel
import me.geckotv.apkmanager.feature.apps.PackagePicker
import me.geckotv.apkmanager.feature.apps.R
import me.geckotv.apkmanager.feature.apps.formatDate
import me.geckotv.apkmanager.feature.apps.isInstalledRelease
import me.geckotv.apkmanager.feature.apps.text

/**
 * The details of the tracked app [appId]: install status and actions, the latest release's notes,
 * settings and the release history.
 *
 * @param onBack called when the user leaves the screen, or when the app is not tracked any more.
 * @param bottomContentPadding extra space at the end of the content, e.g. for the floating navigation toolbar.
 */
@Composable
fun AppDetailsRoute(
    appId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    viewModel: AppDetailsViewModel =
        hiltViewModel<AppDetailsViewModel, AppDetailsViewModel.Factory>(
            key = "app-details-$appId",
            creationCallback = { factory -> factory.create(appId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentOnBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel, resources) {
        viewModel.events.collect { event ->
            when (event) {
                AppDetailsEvent.Closed -> {
                    currentOnBack()
                }

                is AppDetailsEvent.ShowMessage -> {
                    launch {
                        snackbarHostState.showSnackbar(
                            event.message.text(resources),
                        )
                    }
                }
            }
        }
    }

    AppDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onInstallClick = viewModel::install,
        onCancelClick = viewModel::cancelInstall,
        onIncludePrereleasesChange = viewModel::setIncludePrereleases,
        onInstalledAppClick = viewModel::showPackagePicker,
        onPackageSelect = viewModel::setPackageName,
        onPickerBack = viewModel::closePackagePicker,
        onRetryHistoryClick = viewModel::retryHistory,
        onReleaseClick = viewModel::toggleRelease,
        onRemoveClick = viewModel::showRemoveConfirmation,
        onRemoveConfirm = viewModel::remove,
        onRemoveDismiss = viewModel::dismissRemoveConfirmation,
        options =
            AppDetailsOptionsActions(
                onSkipClick = viewModel::skipLatestRelease,
                onUndoSkipClick = viewModel::undoSkip,
                onAssetFilterClick = viewModel::showAssetFilter,
                onAssetFilterSave = viewModel::saveAssetFilter,
                onVersionPatternClick = viewModel::showVersionPattern,
                onVersionPatternSave = viewModel::saveVersionPattern,
                onDismiss = viewModel::dismissOptionsSheet,
            ),
        modifier = modifier,
        bottomContentPadding = bottomContentPadding,
    )
}

/**
 * @param bottomContentPadding extra space at the end of the content, e.g. for the floating navigation toolbar.
 * @param showProgressIndicator whether to show indicators of unknown progress; they animate forever,
 * which screenshot tests cannot wait for.
 */
@Composable
internal fun AppDetailsScreen(
    uiState: AppDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onInstallClick: () -> Unit,
    onCancelClick: () -> Unit,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onInstalledAppClick: () -> Unit,
    onPackageSelect: (String?) -> Unit,
    onPickerBack: () -> Unit,
    onRetryHistoryClick: () -> Unit,
    onReleaseClick: (Long) -> Unit,
    onRemoveClick: () -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
    options: AppDetailsOptionsActions,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    showProgressIndicator: Boolean = true,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ready = uiState as? AppDetailsUiState.Ready
    val app = ready?.item?.app
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ApkManagerTopAppBar(
                title = app?.displayName.orEmpty(),
                subtitle = app?.repository?.fullName,
                navigationIcon = { BackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = bottomContentPadding))
        },
    ) { innerPadding ->
        if (ready != null) {
            AppDetailsList(
                state = ready,
                onInstallClick = onInstallClick,
                onCancelClick = onCancelClick,
                onIncludePrereleasesChange = onIncludePrereleasesChange,
                onInstalledAppClick = onInstalledAppClick,
                onRetryHistoryClick = onRetryHistoryClick,
                onReleaseClick = onReleaseClick,
                onRemoveClick = onRemoveClick,
                options = options,
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding() + bottomContentPadding + 16.dp,
                    ),
                showProgressIndicator = showProgressIndicator,
            )
        }
    }
    if (ready != null) {
        AppDetailsSheets(
            state = ready,
            onPackageSelect = onPackageSelect,
            onPickerBack = onPickerBack,
            onRemoveConfirm = onRemoveConfirm,
            onRemoveDismiss = onRemoveDismiss,
            options = options,
        )
    }
}

@Composable
private fun AppDetailsList(
    state: AppDetailsUiState.Ready,
    onInstallClick: () -> Unit,
    onCancelClick: () -> Unit,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onInstalledAppClick: () -> Unit,
    onRetryHistoryClick: () -> Unit,
    onReleaseClick: (Long) -> Unit,
    onRemoveClick: () -> Unit,
    options: AppDetailsOptionsActions,
    contentPadding: PaddingValues,
    showProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val app = state.item.app
    val whatsNewTitle = stringResource(R.string.feature_apps_details_whats_new)
    val settingsTitle = stringResource(R.string.feature_apps_details_settings)
    val advancedTitle = stringResource(R.string.feature_apps_details_advanced)
    val historyTitle = stringResource(R.string.feature_apps_details_history)
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header", contentType = "Header") {
            AppDetailsHeader(
                item = state.item,
                onInstallClick = onInstallClick,
                onCancelClick = onCancelClick,
                onSkipClick = options.onSkipClick,
                onUndoSkipClick = options.onUndoSkipClick,
                modifier = Modifier.padding(top = 8.dp),
                showProgressIndicator = showProgressIndicator,
            )
        }
        app.latestRelease?.let { release ->
            section(items = listOf(release), title = whatsNewTitle, key = { "whats-new" }) {
                WhatsNewCard(it, isInstalled = app.isInstalledRelease(it))
            }
        }
        section(items = SettingsRow.entries, title = settingsTitle, key = { it }) { row ->
            SettingsItem(
                row = row,
                app = app,
                onIncludePrereleasesChange = onIncludePrereleasesChange,
                onInstalledAppClick = onInstalledAppClick,
                onOpenRepositoryClick = { uriHandler.openUri(app.repository.htmlUrl) },
            )
        }
        advancedSection(title = advancedTitle, app = app, options = options)
        releaseHistory(
            title = historyTitle,
            state = state,
            onRetryClick = onRetryHistoryClick,
            onReleaseClick = onReleaseClick,
            onOpenReleaseClick = { release -> uriHandler.openUri(release.htmlUrl) },
            showProgressIndicator = showProgressIndicator,
        )
        item(key = "remove", contentType = "Remove") {
            RemoveButton(onClick = onRemoveClick, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

private enum class SettingsRow {
    IncludePrereleases,
    InstalledApp,
    OpenRepository,
}

@Composable
private fun SettingsItem(
    row: SettingsRow,
    app: TrackedApp,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onInstalledAppClick: () -> Unit,
    onOpenRepositoryClick: () -> Unit,
) {
    when (row) {
        SettingsRow.IncludePrereleases -> {
            SwitchSectionItem(
                headline = stringResource(R.string.feature_apps_include_prereleases),
                checked = app.includePrereleases,
                onCheckedChange = onIncludePrereleasesChange,
                supportingText = stringResource(R.string.feature_apps_include_prereleases_summary),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Upgrade) },
            )
        }

        SettingsRow.InstalledApp -> {
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

        SettingsRow.OpenRepository -> {
            SectionItem(
                headline = stringResource(R.string.feature_apps_details_open_github),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Code) },
                trailingContent = {
                    Icon(painter = painterResource(ApkManagerIcons.OpenInNew), contentDescription = null)
                },
                onClick = onOpenRepositoryClick,
            )
        }
    }
}

/** The latest [release]: its title, date and notes, and whether it is the one APK Manager installed. */
@Composable
private fun WhatsNewCard(
    release: Release,
    isInstalled: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ListItemDefaults.segmentedShapes(0, 1).shape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = release.tagAndDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isInstalled) InfoLabel(text = stringResource(R.string.feature_apps_details_installed_label))
                if (release.isPrerelease) InfoLabel(text = stringResource(R.string.feature_apps_pre_release))
            }
            if (release.body.isNullOrBlank()) {
                ReleaseNotesOrNone(notes = null, modifier = Modifier.padding(bottom = 8.dp))
            } else {
                CollapsibleReleaseNotes(markdown = release.body.orEmpty(), containerColor = containerColor)
            }
        }
    }
}

/** The tag, if the title does not show it already, and the publication date, e.g. "v1.2.0 · Nov 20, 2026". */
@Composable
private fun Release.tagAndDate(): String =
    listOfNotNull(
        tagName.takeIf { !name.isNullOrBlank() && name != tagName },
        publishedAt?.let { stringResource(R.string.feature_apps_release_published, formatDate(it)) },
    ).joinToString(separator = " · ")

/**
 * The release history as a section: its state, or one expandable item per release. A list scope builder takes
 * the same state and callbacks as a composable; bundling them would only add a class.
 */
@Suppress("LongParameterList")
private fun LazyListScope.releaseHistory(
    title: String,
    state: AppDetailsUiState.Ready,
    onRetryClick: () -> Unit,
    onReleaseClick: (Long) -> Unit,
    onOpenReleaseClick: (Release) -> Unit,
    showProgressIndicator: Boolean,
) {
    val history = state.history
    when {
        history is ReleaseHistoryState.Loaded && history.releases.isNotEmpty() -> {
            val entries =
                history.releases.mapIndexed {
                    index,
                    release,
                    ->
                    HistoryEntry(release, index, history.releases.size)
                }
            section(
                items = entries,
                title = title,
                key = { "release-${it.release.id}" },
                headerAction = {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.feature_apps_details_history_count,
                                entries.size,
                                entries.size,
                            ),
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            ) { entry ->
                ReleaseHistoryItem(
                    entry = entry,
                    isExpanded = entry.release.id == state.expandedReleaseId,
                    isInstalled = state.item.app.isInstalledRelease(entry.release),
                    onClick = { onReleaseClick(entry.release.id) },
                    onOpenClick = { onOpenReleaseClick(entry.release) },
                )
            }
        }

        else -> {
            item(key = "history-header", contentType = "SectionHeader") { SectionHeader(text = title) }
            item(key = "history-state", contentType = "HistoryState") {
                when (history) {
                    ReleaseHistoryState.Loading -> ReleaseHistoryLoading(showProgressIndicator)
                    is ReleaseHistoryState.Failed -> ReleaseHistoryFailed(history.error, onRetryClick)
                    is ReleaseHistoryState.Loaded -> ReleaseHistoryEmpty()
                }
            }
        }
    }
}

@Composable
private fun RemoveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
        Text(text = stringResource(R.string.feature_apps_details_remove_app), modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * The sheets of the screen: the installed apps to link, the confirmation to remove the app, and the
 * editors of the advanced options.
 */
@Composable
private fun AppDetailsSheets(
    state: AppDetailsUiState.Ready,
    onPackageSelect: (String?) -> Unit,
    onPickerBack: () -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
    options: AppDetailsOptionsActions,
) {
    val app = state.item.app
    state.installedApps?.let { installedApps ->
        ApkManagerBottomSheet(onDismissRequest = onPickerBack) {
            PackagePicker(
                installedApps = installedApps,
                selectedPackageName = app.packageName,
                onSelect = onPackageSelect,
                onBackClick = onPickerBack,
            )
        }
    }
    if (state.isConfirmingRemove) {
        ApkManagerBottomSheet(onDismissRequest = onRemoveDismiss) {
            RemoveConfirmation(name = app.displayName, onCancelClick = onRemoveDismiss, onRemoveClick = onRemoveConfirm)
        }
    }
    OptionsSheets(state = state, options = options)
}
