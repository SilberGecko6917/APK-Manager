package me.geckotv.apkmanager.feature.account

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.auth.SignInFailure
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerTopAppBar
import me.geckotv.apkmanager.core.designsystem.component.BackButton
import me.geckotv.apkmanager.core.designsystem.component.ButtonGroupOption
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionHeader
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SingleChoiceButtonGroup
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.network.model.RateLimit
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val DEVICE_CODE_LABEL = "GitHub device code"

/** Screen to sign in to GitHub and manage the signed-in account. */
@Composable
fun AccountRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    AccountScreen(
        uiState = uiState,
        onBack = onBack,
        onAccessLevelSelect = viewModel::selectAccessLevel,
        onSignInClick = viewModel::startDeviceFlow,
        onUseTokenClick = viewModel::showTokenSheet,
        onSignInWithToken = viewModel::signInWithToken,
        onCopyCodeAndOpen = { sheet ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(DEVICE_CODE_LABEL, sheet.userCode)))
            }
            uriHandler.openUri(sheet.verificationUri)
        },
        onCancelSignIn = viewModel::cancelSignIn,
        onDismissSheet = viewModel::dismissSheet,
        onSignOut = viewModel::signOut,
        onDismissError = viewModel::clearError,
        modifier = modifier,
    )
}

@Composable
internal fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onAccessLevelSelect: (GitHubAccessLevel) -> Unit,
    onSignInClick: () -> Unit,
    onUseTokenClick: () -> Unit,
    onSignInWithToken: (String) -> Unit,
    onCopyCodeAndOpen: (AccountSheet.DeviceCode) -> Unit,
    onCancelSignIn: () -> Unit,
    onDismissSheet: () -> Unit,
    onSignOut: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ApkManagerTopAppBar(
                title = stringResource(R.string.feature_account_title),
                navigationIcon = { BackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
        ) {
            if (uiState is AccountUiState.Ready) {
                item(key = "content") {
                    AccountContent(
                        state = uiState,
                        onAccessLevelSelect = onAccessLevelSelect,
                        onSignInClick = onSignInClick,
                        onUseTokenClick = onUseTokenClick,
                        onSignOut = onSignOut,
                        onDismissError = onDismissError,
                    )
                }
            }
        }
    }
    if (uiState is AccountUiState.Ready && uiState.sheet != null) {
        AccountSheetHost(
            sheet = uiState.sheet,
            isWorking = uiState.isWorking,
            error = uiState.error,
            onCopyCodeAndOpen = onCopyCodeAndOpen,
            onCancelSignIn = onCancelSignIn,
            onSignInWithToken = onSignInWithToken,
            onDismiss = onDismissSheet,
        )
    }
}

@Composable
private fun AccountContent(
    state: AccountUiState.Ready,
    onAccessLevelSelect: (GitHubAccessLevel) -> Unit,
    onSignInClick: () -> Unit,
    onUseTokenClick: () -> Unit,
    onSignOut: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // The token sheet shows its own error next to the input field.
        if (state.error != null && state.sheet != AccountSheet.Token) {
            ErrorItem(failure = state.error, onDismiss = onDismissError)
        }
        when (val authState = state.authState) {
            AuthState.SignedOut -> {
                SignedOutContent(
                    accessLevel = state.accessLevel,
                    isWorking = state.isWorking,
                    onAccessLevelSelect = onAccessLevelSelect,
                    onSignInClick = onSignInClick,
                    onUseTokenClick = onUseTokenClick,
                )
            }

            is AuthState.SignedIn -> {
                SignedInContent(
                    authState = authState,
                    rateLimit = state.rateLimit,
                    onSignInAgain = onSignInClick,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun ErrorItem(
    failure: SignInFailure,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier.padding(top = 8.dp)) {
        item {
            SectionItem(
                headline = stringResource(failure.messageRes()),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Error) },
                trailingContent = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.feature_account_dismiss_error))
                    }
                },
            )
        }
    }
}

@Composable
private fun SignedOutContent(
    accessLevel: GitHubAccessLevel,
    isWorking: Boolean,
    onAccessLevelSelect: (GitHubAccessLevel) -> Unit,
    onSignInClick: () -> Unit,
    onUseTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.feature_account_intro),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionHeader(text = stringResource(R.string.feature_account_access))
        SingleChoiceButtonGroup(
            options =
                listOf(
                    ButtonGroupOption(stringResource(R.string.feature_account_access_public), ApkManagerIcons.Code),
                    ButtonGroupOption(stringResource(R.string.feature_account_access_private), ApkManagerIcons.Lock),
                ),
            selectedIndex = GitHubAccessLevel.entries.indexOf(accessLevel),
            onOptionSelect = { index -> onAccessLevelSelect(GitHubAccessLevel.entries[index]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text =
                stringResource(
                    when (accessLevel) {
                        GitHubAccessLevel.Public -> R.string.feature_account_access_public_summary
                        GitHubAccessLevel.Private -> R.string.feature_account_access_private_summary
                    },
                ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !isWorking,
        ) {
            Text(stringResource(R.string.feature_account_sign_in))
        }
        Section(
            modifier = Modifier.padding(top = 8.dp),
            title = stringResource(R.string.feature_account_other_options),
        ) {
            item {
                SectionItem(
                    headline = stringResource(R.string.feature_account_use_token),
                    supportingText = stringResource(R.string.feature_account_use_token_summary),
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Key) },
                    onClick = onUseTokenClick,
                )
            }
        }
    }
}

@StringRes
internal fun SignInFailure.messageRes(): Int =
    when (this) {
        SignInFailure.Expired -> R.string.feature_account_error_expired
        SignInFailure.Denied -> R.string.feature_account_error_denied
        SignInFailure.InvalidToken -> R.string.feature_account_error_invalid_token
        SignInFailure.Network -> R.string.feature_account_error_network
        SignInFailure.Unknown -> R.string.feature_account_error_unknown
    }

@Preview(name = "Signed out")
@Composable
private fun AccountScreenSignedOutPreview() {
    ApkManagerTheme(darkTheme = false, dynamicColor = false) {
        AccountScreenPreviewContent(previewState(AuthState.SignedOut))
    }
}

@Preview(name = "Signed in, dark")
@Composable
private fun AccountScreenSignedInPreview() {
    ApkManagerTheme(darkTheme = true, dynamicColor = false) {
        AccountScreenPreviewContent(
            previewState(
                AuthState.SignedIn(
                    GitHubAccount(
                        login = "octocat",
                        name = "The Octocat",
                        avatarUrl = null,
                        authMethod = AuthMethod.OAuth,
                        accessLevel = GitHubAccessLevel.Private,
                    ),
                ),
                rateLimit = RateLimit("core", limit = 5_000, remaining = 4_987, resetAt = Instant.now()),
            ),
        )
    }
}

private fun previewState(
    authState: AuthState,
    rateLimit: RateLimit? = null,
) = AccountUiState.Ready(
    authState = authState,
    rateLimit = rateLimit,
    accessLevel = GitHubAccessLevel.Private,
    sheet = null,
    isWorking = false,
    error = null,
)

@Composable
private fun AccountScreenPreviewContent(state: AccountUiState) {
    AccountScreen(
        uiState = state,
        onBack = {},
        onAccessLevelSelect = {},
        onSignInClick = {},
        onUseTokenClick = {},
        onSignInWithToken = {},
        onCopyCodeAndOpen = {},
        onCancelSignIn = {},
        onDismissSheet = {},
        onSignOut = {},
        onDismissError = {},
    )
}
