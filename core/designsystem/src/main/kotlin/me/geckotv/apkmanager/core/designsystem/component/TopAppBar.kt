package me.geckotv.apkmanager.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.R
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons

/**
 * The large, collapsing top app bar used by top-level and detail screens.
 *
 * Pass a [scrollBehavior] (e.g. `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`) and connect
 * it to the screen's scrolling content to collapse the bar on scroll.
 */
@Composable
fun ApkManagerTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeFlexibleTopAppBar(
        title = { Text(text = title) },
        modifier = modifier.padding(horizontal = 8.dp),
        subtitle = subtitle?.let { { Text(text = it) } },
        navigationIcon = navigationIcon,
        actions = actions,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        scrollBehavior = scrollBehavior,
    )
}

/** Navigation icon that navigates back. */
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(ApkManagerIcons.ArrowBack),
            contentDescription = stringResource(R.string.core_designsystem_navigate_back),
        )
    }
}
