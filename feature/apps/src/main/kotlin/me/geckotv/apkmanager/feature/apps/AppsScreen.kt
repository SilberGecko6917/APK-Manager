package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerTopAppBar
import me.geckotv.apkmanager.core.designsystem.component.section
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme

/**
 * The tracked apps, grouped by update status, with the sheets to add an app and to act on one.
 *
 * @param showAddAppSheet whether the sheet to add an app is open.
 * @param onAddAppSheetDismiss called when the user closes the sheet to add an app.
 * @param onAddAppClick called when the user wants to add an app, e.g. from the empty state.
 * @param bottomContentPadding extra space at the end of the content, e.g. for the floating navigation toolbar.
 */
@Composable
fun AppsRoute(
    showAddAppSheet: Boolean,
    onAddAppSheetDismiss: () -> Unit,
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message.text(resources)) }
    }

    AppsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onAppClick = viewModel::selectApp,
        onAddAppClick = onAddAppClick,
        modifier = modifier,
        bottomContentPadding = bottomContentPadding,
    )

    if (showAddAppSheet) {
        AddAppSheet(
            onDismiss = onAddAppSheetDismiss,
            onAppAdd = { name ->
                onAddAppSheetDismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(resources.getString(R.string.feature_apps_message_added, name))
                }
            },
        )
    }
    (uiState as? AppsUiState.Ready)?.actions?.let { actions ->
        AppActionsSheet(
            state = actions,
            onDismiss = viewModel::dismissApp,
            onIncludePrereleasesChange = viewModel::setIncludePrereleases,
            onInstalledAppClick = viewModel::showPackagePicker,
            onPackageSelect = viewModel::setPackageName,
            onPickerBack = viewModel::closePackagePicker,
            onRemoveConfirm = viewModel::removeApp,
        )
    }
}

@Composable
internal fun AppsScreen(
    uiState: AppsUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onAppClick: (Long) -> Unit,
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ApkManagerTopAppBar(
                title = stringResource(R.string.feature_apps_title),
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = bottomContentPadding))
        },
    ) { innerPadding ->
        when {
            uiState !is AppsUiState.Ready -> {
                Box(modifier = Modifier.fillMaxSize())
            }

            uiState.groups.isEmpty() -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(bottom = bottomContentPadding)
                            .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    AppsEmptyState(onAddAppClick = onAddAppClick, modifier = Modifier.padding(32.dp))
                }
            }

            else -> {
                AppsList(
                    state = uiState,
                    onRefresh = onRefresh,
                    onAppClick = onAppClick,
                    modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                    bottomPadding = innerPadding.calculateBottomPadding() + bottomContentPadding,
                )
            }
        }
    }
}

@Composable
private fun AppsList(
    state: AppsUiState.Ready,
    onRefresh: () -> Unit,
    onAppClick: (Long) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val headers =
        state.groups.map { group ->
            stringResource(
                R.string.feature_apps_group_header,
                stringResource(group.kind.titleRes()),
                group.apps.size,
            )
        }
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp),
        ) {
            state.groups.forEachIndexed { index, group ->
                section(items = group.apps, title = headers[index], key = { it.id }) { app ->
                    AppListItem(app = app, onClick = { onAppClick(app.id) })
                }
            }
        }
    }
}

private fun AppGroupKind.titleRes(): Int =
    when (this) {
        AppGroupKind.UpdatesAvailable -> R.string.feature_apps_group_updates
        AppGroupKind.UpToDate -> R.string.feature_apps_group_up_to_date
        AppGroupKind.NotInstalled -> R.string.feature_apps_group_not_installed
    }

@Composable
private fun AppsEmptyState(
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(ApkManagerIcons.Apps),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.feature_apps_empty_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.feature_apps_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onAddAppClick) {
            Icon(
                painter = painterResource(ApkManagerIcons.Add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.feature_apps_empty_add),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** The button of the floating toolbar that opens the sheet to add an app. */
@Composable
fun AddAppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(ApkManagerIcons.Add),
            contentDescription = stringResource(R.string.feature_apps_add_content_description),
        )
    }
}

@Preview(name = "Light")
@Composable
private fun AppsEmptyLightPreview() {
    ApkManagerTheme(darkTheme = false, dynamicColor = false) {
        AppsScreen(
            uiState = AppsUiState.Ready(groups = emptyList(), isRefreshing = false, actions = null),
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            onAppClick = {},
            onAddAppClick = {},
        )
    }
}

@Preview(name = "Dark")
@Composable
private fun AppsEmptyDarkPreview() {
    ApkManagerTheme(darkTheme = true, dynamicColor = false) {
        AppsScreen(
            uiState = AppsUiState.Ready(groups = emptyList(), isRefreshing = false, actions = null),
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            onAppClick = {},
            onAddAppClick = {},
        )
    }
}
