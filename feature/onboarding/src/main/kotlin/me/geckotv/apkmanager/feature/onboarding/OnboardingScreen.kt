package me.geckotv.apkmanager.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.Section
import me.geckotv.apkmanager.core.designsystem.component.SectionItem
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme

/**
 * Welcome screen that explains the app and offers to grant the optional permissions.
 *
 * @param notificationPermissionGranted `null` hides the notifications row (no permission needed).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    installPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean?,
    onRequestInstallPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header()
            Spacer(Modifier.height(32.dp))
            PermissionsSection(
                installPermissionGranted = installPermissionGranted,
                notificationPermissionGranted = notificationPermissionGranted,
                onRequestInstallPermission = onRequestInstallPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
            Text(
                text = stringResource(R.string.feature_onboarding_permissions_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_onboarding_get_started))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(ApkManagerIcons.Apps),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.feature_onboarding_title),
            style = MaterialTheme.typography.headlineLargeEmphasized,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.feature_onboarding_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionsSection(
    installPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean?,
    onRequestInstallPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Section(modifier = modifier, title = stringResource(R.string.feature_onboarding_permissions_title)) {
        item {
            SectionItem(
                headline = stringResource(R.string.feature_onboarding_install_title),
                supportingText = stringResource(R.string.feature_onboarding_install_description),
                leadingContent = { SectionItemIcon(ApkManagerIcons.Download) },
                trailingContent = { PermissionAction(installPermissionGranted, onRequestInstallPermission) },
            )
        }
        if (notificationPermissionGranted != null) {
            item {
                SectionItem(
                    headline = stringResource(R.string.feature_onboarding_notifications_title),
                    supportingText = stringResource(R.string.feature_onboarding_notifications_description),
                    leadingContent = { SectionItemIcon(ApkManagerIcons.Notifications) },
                    trailingContent = {
                        PermissionAction(notificationPermissionGranted, onRequestNotificationPermission)
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionAction(
    granted: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (granted) {
        Icon(
            painter = painterResource(ApkManagerIcons.CheckCircle),
            contentDescription = stringResource(R.string.feature_onboarding_allowed),
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary,
        )
    } else {
        FilledTonalButton(onClick = onRequest, modifier = modifier) {
            Text(stringResource(R.string.feature_onboarding_allow))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenLightPreview() {
    ApkManagerTheme(darkTheme = false, dynamicColor = false) {
        OnboardingScreen(false, false, {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenDarkPreview() {
    ApkManagerTheme(darkTheme = true, dynamicColor = false) {
        OnboardingScreen(true, false, {}, {}, {})
    }
}
