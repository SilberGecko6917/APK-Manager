package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerBottomSheet
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.SourceError

/** Bottom sheet to look up a GitHub repository and track it. [onAppAdd] is called with the new app's name. */
@Composable
internal fun AddAppSheet(
    onDismiss: () -> Unit,
    onAppAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddAppViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val currentOnAppAdd by rememberUpdatedState(onAppAdd)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddAppEvent.Added -> currentOnAppAdd(event.name)
            }
        }
    }

    val step = state.step
    ApkManagerBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        modifier = modifier,
        // The package picker has its own header with a back button.
        title =
            if (step is AddAppStep.Preview &&
                step.isPickingPackage
            ) {
                null
            } else {
                stringResource(R.string.feature_apps_add_title)
            },
    ) {
        AddAppSheetContent(
            state = state,
            onInputChange = viewModel::setInput,
            onPasteClick = {
                scope.launch {
                    val text =
                        clipboard
                            .getClipEntry()
                            ?.clipData
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.text
                    if (text != null) viewModel.setInput(text.toString())
                }
            },
            onLookUpClick = viewModel::lookUp,
            onBackClick = viewModel::back,
            onIncludePrereleasesChange = viewModel::setIncludePrereleases,
            onAssetSelect = viewModel::selectAsset,
            onInstalledAppClick = viewModel::showPackagePicker,
            onPackageSelect = viewModel::selectPackage,
            onAddClick = viewModel::add,
        )
    }
}

/** The content of the sheet to add an app: one view per [AddAppStep]. */
@Composable
internal fun AddAppSheetContent(
    state: AddAppUiState,
    onInputChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onLookUpClick: () -> Unit,
    onBackClick: () -> Unit,
    onIncludePrereleasesChange: (Boolean) -> Unit,
    onAssetSelect: (String) -> Unit,
    onInstalledAppClick: () -> Unit,
    onPackageSelect: (String?) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Screenshot tests turn the endless animation off, because Compose never becomes idle while it runs.
    showProgressIndicator: Boolean = true,
) {
    when (val step = state.step) {
        AddAppStep.Input -> {
            AddAppInput(
                input = state.input,
                error = state.inputError,
                onInputChange = onInputChange,
                onPasteClick = onPasteClick,
                onLookUpClick = onLookUpClick,
                modifier = modifier,
            )
        }

        AddAppStep.Loading -> {
            AddAppLoading(showProgressIndicator = showProgressIndicator, modifier = modifier)
        }

        is AddAppStep.Failed -> {
            AddAppFailed(
                error = step.error,
                onRetryClick = onLookUpClick,
                onBackClick = onBackClick,
                modifier = modifier,
            )
        }

        is AddAppStep.Preview -> {
            if (step.isPickingPackage) {
                PackagePicker(
                    installedApps = step.installedApps,
                    selectedPackageName = step.packageName,
                    onSelect = onPackageSelect,
                    onBackClick = onBackClick,
                    modifier = modifier,
                )
            } else {
                AddAppPreview(
                    step = step,
                    onIncludePrereleasesChange = onIncludePrereleasesChange,
                    onAssetSelect = onAssetSelect,
                    onInstalledAppClick = onInstalledAppClick,
                    onBackClick = onBackClick,
                    onAddClick = onAddClick,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun AddAppInput(
    input: String,
    error: AddAppInputError?,
    onInputChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onLookUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_apps_add_input_label)) },
            placeholder = { Text(stringResource(R.string.feature_apps_add_input_placeholder)) },
            trailingIcon = {
                IconButton(onClick = onPasteClick) {
                    Icon(
                        painter = painterResource(ApkManagerIcons.ContentPaste),
                        contentDescription = stringResource(R.string.feature_apps_add_paste),
                    )
                }
            },
            supportingText =
                error?.let {
                    {
                        Text(
                            stringResource(
                                when (it) {
                                    AddAppInputError.Invalid -> R.string.feature_apps_add_error_invalid
                                    AddAppInputError.AlreadyTracked -> R.string.feature_apps_add_error_tracked
                                },
                            ),
                        )
                    }
                },
            isError = error != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (input.isNotBlank()) onLookUpClick() }),
        )
        Button(
            onClick = onLookUpClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank(),
        ) {
            Text(stringResource(R.string.feature_apps_add_look_up))
        }
    }
}

@Composable
private fun AddAppLoading(
    showProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showProgressIndicator) LoadingIndicator()
        Text(
            text = stringResource(R.string.feature_apps_add_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddAppFailed(
    error: SourceError,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = error.lookUpMessages()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionItemIcon(ApkManagerIcons.Error)
        Text(
            text = messages.first(),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        messages.drop(1).forEach { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBackClick) { Text(stringResource(R.string.feature_apps_add_back)) }
            Button(onClick = onRetryClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_apps_error_retry))
            }
        }
    }
}
