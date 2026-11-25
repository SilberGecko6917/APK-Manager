package me.geckotv.apkmanager.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.ApkManagerTopAppBar
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.designsystem.theme.ApkManagerTheme

/**
 * List of tracked apps. Tracking apps is not implemented yet, so the screen shows its empty state.
 *
 * @param bottomContentPadding extra space at the end of the content, e.g. for the floating navigation toolbar.
 */
@Composable
fun AppsScreen(
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
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = bottomContentPadding)
                    .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            AppsEmptyState(modifier = Modifier.padding(32.dp))
        }
    }
}

@Composable
private fun AppsEmptyState(modifier: Modifier = Modifier) {
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
    }
}

@Preview(name = "Light")
@Composable
private fun AppsScreenLightPreview() {
    ApkManagerTheme(darkTheme = false, dynamicColor = false) { AppsScreen() }
}

@Preview(name = "Dark")
@Composable
private fun AppsScreenDarkPreview() {
    ApkManagerTheme(darkTheme = true, dynamicColor = false) { AppsScreen() }
}
