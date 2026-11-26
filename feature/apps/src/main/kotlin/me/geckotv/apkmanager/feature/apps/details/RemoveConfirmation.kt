package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.feature.apps.R

/** Asks the user to confirm that app [name] is removed from the tracked apps. */
@Composable
internal fun RemoveConfirmation(
    name: String,
    onCancelClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.feature_apps_details_remove_title, name),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
            text = stringResource(R.string.feature_apps_details_remove_message),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancelClick) { Text(stringResource(R.string.feature_apps_details_cancel)) }
            Button(
                onClick = onRemoveClick,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text(stringResource(R.string.feature_apps_details_remove))
            }
        }
    }
}
