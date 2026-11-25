package me.geckotv.apkmanager.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerBottomSheet

@Composable
internal fun AccountSheetHost(
    sheet: AccountSheet,
    isWorking: Boolean,
    error: SignInFailure?,
    onCopyCodeAndOpen: (AccountSheet.DeviceCode) -> Unit,
    onCancelSignIn: () -> Unit,
    onSignInWithToken: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (sheet) {
        is AccountSheet.DeviceCode -> {
            ApkManagerBottomSheet(
                onDismissRequest = onCancelSignIn,
                modifier = modifier,
                title = stringResource(R.string.feature_account_device_code_title),
            ) {
                DeviceCodeSheetContent(
                    userCode = sheet.userCode,
                    onCopyCodeAndOpen = { onCopyCodeAndOpen(sheet) },
                    onCancel = onCancelSignIn,
                )
            }
        }

        AccountSheet.Token -> {
            ApkManagerBottomSheet(
                onDismissRequest = onDismiss,
                modifier = modifier,
                title = stringResource(R.string.feature_account_token_label),
            ) {
                TokenSheetContent(
                    isWorking = isWorking,
                    error = error,
                    onSignIn = onSignInWithToken,
                )
            }
        }
    }
}

@Composable
internal fun DeviceCodeSheetContent(
    userCode: String,
    onCopyCodeAndOpen: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    // Screenshot tests turn the endless animation off, because Compose never becomes idle while it runs.
    showProgressIndicator: Boolean = true,
) {
    val codeDescription = stringResource(R.string.feature_account_code_label)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SelectionContainer {
            Text(
                text = userCode,
                modifier =
                    Modifier
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = "$codeDescription $userCode" },
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
            )
        }
        Button(onClick = onCopyCodeAndOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.feature_account_copy_and_open))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showProgressIndicator) LoadingIndicator(modifier = Modifier.padding(4.dp))
            Text(
                text = stringResource(R.string.feature_account_waiting),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.feature_account_cancel))
        }
    }
}

@Composable
internal fun TokenSheetContent(
    isWorking: Boolean,
    error: SignInFailure?,
    onSignIn: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var token by rememberSaveable { mutableStateOf("") }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_account_token_label)) },
            supportingText = {
                Text(
                    stringResource(
                        if (error == null) R.string.feature_account_token_hint else error.messageRes(),
                    ),
                )
            },
            isError = error != null,
            singleLine = true,
            enabled = !isWorking,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Button(
            onClick = { onSignIn(token) },
            modifier = Modifier.fillMaxWidth(),
            enabled = token.isNotBlank() && !isWorking,
        ) {
            Text(stringResource(R.string.feature_account_token_sign_in))
        }
    }
}
