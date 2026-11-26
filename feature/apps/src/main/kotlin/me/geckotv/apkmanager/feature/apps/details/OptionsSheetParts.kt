package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.model.toRegexOrNull
import me.geckotv.apkmanager.feature.apps.R

/** A text field for a regular expression that shows an error below it if [isError], otherwise the [hint]. */
@Composable
internal fun PatternField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    val supporting: (@Composable () -> Unit)? =
        when {
            isError -> {
                { Text(stringResource(R.string.feature_apps_filter_invalid)) }
            }

            hint != null -> {
                { Text(hint) }
            }

            else -> {
                null
            }
        }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = supporting,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = imeAction),
    )
}

/** Title of a sheet that edits an option. */
@Composable
internal fun SheetTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 8.dp),
        style = MaterialTheme.typography.headlineSmallEmphasized,
    )
}

/** "Reset" at the start, "Cancel" and "Save" at the end. */
@Composable
internal fun OptionsSheetButtons(
    canReset: Boolean,
    canSave: Boolean,
    onResetClick: () -> Unit,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onResetClick, enabled = canReset) {
            Text(stringResource(R.string.feature_apps_options_reset))
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancelClick) { Text(stringResource(R.string.feature_apps_details_cancel)) }
            Button(onClick = onSaveClick, enabled = canSave) {
                Text(stringResource(R.string.feature_apps_options_save))
            }
        }
    }
}

/** Whether this is a pattern that [toRegexOrNull] cannot compile; a blank text is not a pattern. */
internal fun String?.isInvalidPattern(): Boolean = !isNullOrBlank() && toRegexOrNull() == null
