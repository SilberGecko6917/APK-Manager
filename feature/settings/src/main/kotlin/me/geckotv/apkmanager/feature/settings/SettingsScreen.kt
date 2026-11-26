package me.geckotv.apkmanager.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerTopAppBar
import me.geckotv.apkmanager.core.designsystem.component.ButtonGroupOption
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionHeader
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.component.SingleChoiceButtonGroup
import me.geckotv.apkmanager.core.designsystem.component.SwitchSectionItem
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme
import me.geckotv.apkmanager.core.model.AuthMethod
import me.geckotv.apkmanager.core.model.GitHubAccessLevel
import me.geckotv.apkmanager.core.model.GitHubAccount
import me.geckotv.apkmanager.core.model.ThemeMode
import me.geckotv.apkmanager.core.model.UpdateCheckInterval
import me.geckotv.apkmanager.core.model.UserSettings

private const val SOURCE_CODE_URL = "https://github.com/SilberGecko6917/APK-Manager"
private const val LICENSE_URL = "https://github.com/SilberGecko6917/APK-Manager/blob/main/LICENSE"

/**
 * Settings screen.
 *
 * @param bottomContentPadding extra space at the end of the list, e.g. for the floating navigation toolbar.
 */
@Composable
fun SettingsRoute(
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName =
        remember(context) {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }
    val uriHandler = LocalUriHandler.current

    SettingsScreen(
        uiState = uiState,
        versionName = versionName,
        supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setUseDynamicColor,
        onAmoledBlackChange = viewModel::setUseAmoledBlack,
        onHapticsChange = viewModel::setHapticsEnabled,
        onBackgroundChecksChange = viewModel::setBackgroundChecksEnabled,
        onCheckIntervalChange = viewModel::setCheckInterval,
        onUnmeteredOnlyChange = viewModel::setCheckOnlyOnUnmeteredNetwork,
        onOpenSourceCode = { uriHandler.openUri(SOURCE_CODE_URL) },
        onOpenLicense = { uriHandler.openUri(LICENSE_URL) },
        onOpenAccount = onOpenAccount,
        modifier = modifier,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    versionName: String,
    supportsDynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBackgroundChecksChange: (Boolean) -> Unit,
    onCheckIntervalChange: (UpdateCheckInterval) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    onOpenSourceCode: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ApkManagerTopAppBar(
                title = stringResource(R.string.feature_settings_title),
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
                    bottom = innerPadding.calculateBottomPadding() + bottomContentPadding + 16.dp,
                ),
        ) {
            if (uiState is SettingsUiState.Success) {
                item(key = "account") {
                    AccountSettings(
                        account = uiState.account,
                        needsSignIn = uiState.accountNeedsSignIn,
                        onOpenAccount = onOpenAccount,
                    )
                }
                item(key = "updates") {
                    UpdateSettings(
                        settings = uiState.settings,
                        onBackgroundChecksChange = onBackgroundChecksChange,
                        onCheckIntervalChange = onCheckIntervalChange,
                        onUnmeteredOnlyChange = onUnmeteredOnlyChange,
                    )
                }
                item(key = "appearance") {
                    AppearanceSettings(
                        settings = uiState.settings,
                        supportsDynamicColor = supportsDynamicColor,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onAmoledBlackChange = onAmoledBlackChange,
                        onHapticsChange = onHapticsChange,
                    )
                }
                item(key = "about") {
                    AboutSettings(
                        versionName = versionName,
                        onOpenSourceCode = onOpenSourceCode,
                        onOpenLicense = onOpenLicense,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateSettings(
    settings: UserSettings,
    onBackgroundChecksChange: (Boolean) -> Unit,
    onCheckIntervalChange: (UpdateCheckInterval) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Section(title = stringResource(R.string.feature_settings_updates)) {
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_settings_background_checks),
                    supportingText = stringResource(R.string.feature_settings_background_checks_summary),
                    checked = settings.backgroundChecksEnabled,
                    onCheckedChange = onBackgroundChecksChange,
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Sync) },
                )
            }
            if (settings.backgroundChecksEnabled) {
                item {
                    SwitchSectionItem(
                        headline = stringResource(R.string.feature_settings_unmetered_only),
                        supportingText = stringResource(R.string.feature_settings_unmetered_only_summary),
                        checked = settings.checkOnlyOnUnmeteredNetwork,
                        onCheckedChange = onUnmeteredOnlyChange,
                        leadingContent = { SectionItemIcon(ApkManagerIcons.Schedule) },
                    )
                }
            }
        }
        if (settings.backgroundChecksEnabled) {
            SectionHeader(text = stringResource(R.string.feature_settings_check_interval))
            SingleChoiceButtonGroup(
                options =
                    UpdateCheckInterval.entries.map { interval ->
                        ButtonGroupOption(
                            stringResource(R.string.feature_settings_check_interval_hours, interval.hours),
                        )
                    },
                selectedIndex = settings.checkInterval.ordinal,
                onOptionSelect = { index -> onCheckIntervalChange(UpdateCheckInterval.entries[index]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AppearanceSettings(
    settings: UserSettings,
    supportsDynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(text = stringResource(R.string.feature_settings_appearance))
        SingleChoiceButtonGroup(
            options =
                listOf(
                    ButtonGroupOption(
                        stringResource(R.string.feature_settings_theme_system),
                        ApkManagerIcons.BrightnessAuto,
                    ),
                    ButtonGroupOption(stringResource(R.string.feature_settings_theme_light), ApkManagerIcons.LightMode),
                    ButtonGroupOption(stringResource(R.string.feature_settings_theme_dark), ApkManagerIcons.DarkMode),
                ),
            selectedIndex = settings.themeMode.ordinal,
            onOptionSelect = { index -> onThemeModeChange(ThemeMode.entries[index]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Section(modifier = Modifier.padding(top = 8.dp)) {
            if (supportsDynamicColor) {
                item {
                    SwitchSectionItem(
                        headline = stringResource(R.string.feature_settings_dynamic_color),
                        supportingText = stringResource(R.string.feature_settings_dynamic_color_summary),
                        checked = settings.useDynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        leadingContent = { SectionItemIcon(ApkManagerIcons.Palette) },
                    )
                }
            }
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_settings_pitch_black),
                    supportingText = stringResource(R.string.feature_settings_pitch_black_summary),
                    checked = settings.useAmoledBlack,
                    onCheckedChange = onAmoledBlackChange,
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Contrast) },
                )
            }
            item {
                SwitchSectionItem(
                    headline = stringResource(R.string.feature_settings_haptics),
                    supportingText = stringResource(R.string.feature_settings_haptics_summary),
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Vibration) },
                )
            }
        }
    }
}

@Composable
private fun AccountSettings(
    account: GitHubAccount?,
    needsSignIn: Boolean,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier, title = stringResource(R.string.feature_settings_account)) {
        item {
            SectionItem(
                headline =
                    account?.let { it.name ?: it.login } ?: stringResource(R.string.feature_settings_github_account),
                supportingText =
                    account?.let { accountSummary(it, needsSignIn) }
                        ?: stringResource(R.string.feature_settings_github_account_summary),
                leadingContent = { AccountAvatar(avatarUrl = account?.avatarUrl) },
                trailingContent = {
                    Icon(
                        painter = painterResource(ApkManagerIcons.ChevronRight),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = onOpenAccount,
            )
        }
    }
}

/** `@login` and how the account is signed in, or that it has to sign in again. */
@Composable
private fun accountSummary(
    account: GitHubAccount,
    needsSignIn: Boolean,
): String =
    when {
        needsSignIn -> {
            stringResource(R.string.feature_settings_account_expired, account.login)
        }

        account.authMethod == AuthMethod.PersonalAccessToken -> {
            stringResource(R.string.feature_settings_account_token, account.login)
        }

        account.accessLevel == GitHubAccessLevel.Private -> {
            stringResource(R.string.feature_settings_account_private, account.login)
        }

        else -> {
            stringResource(R.string.feature_settings_account_public, account.login)
        }
    }

/** The GitHub avatar as a circle; the account icon while it loads or when signed out. */
@Composable
private fun AccountAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(AvatarSize)) {
        SectionItemIcon(ApkManagerIcons.AccountCircle, modifier = Modifier.size(AvatarSize))
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(AvatarSize).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private val AvatarSize = 40.dp

@Composable
private fun AboutSettings(
    versionName: String,
    onOpenSourceCode: () -> Unit,
    onOpenLicense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier, title = stringResource(R.string.feature_settings_about)) {
        item {
            SectionItem(
                headline = stringResource(R.string.feature_settings_version),
                supportingText = versionName,
                leadingContent = { SectionItemIcon(ApkManagerIcons.Info) },
            )
        }
        item {
            SectionItem(
                headline = stringResource(R.string.feature_settings_source_code),
                supportingText = stringResource(R.string.feature_settings_source_code_summary),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Code) },
                trailingContent = { OpenInNewIcon() },
                onClick = onOpenSourceCode,
            )
        }
        item {
            SectionItem(
                headline = stringResource(R.string.feature_settings_license),
                supportingText = stringResource(R.string.feature_settings_license_summary),
                leadingContent = { SectionItemIcon(ApkManagerIcons.NewReleases) },
                trailingContent = { OpenInNewIcon() },
                onClick = onOpenLicense,
            )
        }
    }
}

@Composable
private fun OpenInNewIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(ApkManagerIcons.OpenInNew),
        contentDescription = null,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "Light")
@Composable
private fun SettingsScreenLightPreview() {
    ApkManagerTheme(darkTheme = false, dynamicColor = false) { SettingsScreenPreviewContent() }
}

@Preview(name = "Dark")
@Composable
private fun SettingsScreenDarkPreview() {
    ApkManagerTheme(darkTheme = true, dynamicColor = false) { SettingsScreenPreviewContent() }
}

@Composable
private fun SettingsScreenPreviewContent() {
    SettingsScreen(
        uiState = SettingsUiState.Success(UserSettings()),
        versionName = "0.1.0",
        supportsDynamicColor = true,
        onThemeModeChange = {},
        onDynamicColorChange = {},
        onAmoledBlackChange = {},
        onHapticsChange = {},
        onBackgroundChecksChange = {},
        onCheckIntervalChange = {},
        onUnmeteredOnlyChange = {},
        onOpenSourceCode = {},
        onOpenLicense = {},
        onOpenAccount = {},
    )
}
