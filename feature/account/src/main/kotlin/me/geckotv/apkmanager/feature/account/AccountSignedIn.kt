package me.geckotv.apkmanager.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.auth.AuthState
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.network.model.RateLimit
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SignedInContent(
    authState: AuthState.SignedIn,
    rateLimit: RateLimit?,
    onSignInAgain: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Section(modifier = Modifier.padding(top = 8.dp)) {
            item {
                SectionItem(
                    headline = authState.account.login,
                    supportingText = accountSummary(authState.account),
                    leadingContent = { SectionItemIcon(ApkManagerIcons.AccountCircle) },
                )
            }
            if (authState.tokenInvalid) {
                item {
                    SectionItem(
                        headline = stringResource(R.string.feature_account_token_invalid),
                        supportingText = stringResource(R.string.feature_account_token_invalid_summary),
                        leadingContent = { SectionItemIcon(ApkManagerIcons.Error) },
                        onClick = onSignInAgain,
                    )
                }
            }
        }
        RateLimitSection(rateLimit = rateLimit)
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(R.string.feature_account_sign_out))
        }
    }
}

@Composable
private fun RateLimitSection(
    rateLimit: RateLimit?,
    modifier: Modifier = Modifier,
) {
    val numberFormat = NumberFormat.getIntegerInstance()
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    Section(modifier = modifier, title = stringResource(R.string.feature_account_rate_limit)) {
        item {
            SectionItem(
                headline = stringResource(R.string.feature_account_remaining),
                supportingText =
                    rateLimit?.let {
                        stringResource(
                            R.string.feature_account_remaining_value,
                            numberFormat.format(it.remaining),
                            numberFormat.format(it.limit),
                        )
                    } ?: stringResource(R.string.feature_account_unknown),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Sync) },
            )
        }
        if (rateLimit != null) {
            item {
                SectionItem(
                    headline = stringResource(R.string.feature_account_resets),
                    supportingText = timeFormat.format(rateLimit.resetAt),
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Schedule) },
                )
            }
        }
    }
}

@Composable
private fun accountSummary(account: GitHubAccount): String {
    val method =
        stringResource(
            when (account.authMethod) {
                AuthMethod.OAuth -> R.string.feature_account_method_oauth
                AuthMethod.PersonalAccessToken -> R.string.feature_account_method_token
            },
        )
    val detail =
        when (account.accessLevel) {
            GitHubAccessLevel.Public -> stringResource(R.string.feature_account_level_public)
            GitHubAccessLevel.Private -> stringResource(R.string.feature_account_level_private)
            null -> null
        }
    val methodAndLevel =
        if (detail == null) method else stringResource(R.string.feature_account_account_summary, method, detail)
    return listOfNotNull(account.name, methodAndLevel).joinToString(separator = "\n")
}
