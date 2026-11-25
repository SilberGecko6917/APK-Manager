package me.geckotv.apkmanager.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A top-level destination shown in the [NavigationToolbar]. */
@Immutable
data class NavigationToolbarItem(
    val label: String,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int = icon,
)

/**
 * Floating toolbar used as the app's main navigation. The selected destination expands to show its
 * label.
 *
 * @param floatingActionButton optional FAB shown next to the toolbar, e.g. "Add app".
 */
@Composable
fun NavigationToolbar(
    items: List<NavigationToolbarItem>,
    selectedIndex: Int,
    onItemSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: (@Composable () -> Unit)? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        items.forEachIndexed { index, item ->
            NavigationToolbarButton(
                item = item,
                selected = index == selectedIndex,
                onClick = { onItemSelect(index) },
            )
        }
    }
    if (floatingActionButton != null) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = floatingActionButton,
            modifier = modifier,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            content = content,
        )
    } else {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = modifier,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            content = content,
        )
    }
}

@Composable
private fun NavigationToolbarButton(
    item: NavigationToolbarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    ToggleButton(
        checked = selected,
        onCheckedChange = {
            if (!selected) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
        },
        modifier = modifier.semantics { role = Role.Tab },
        colors =
            ToggleButtonDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
    ) {
        Icon(
            painter = painterResource(if (selected) item.selectedIcon else item.icon),
            contentDescription = if (selected) null else item.label,
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Text(text = item.label, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
        }
    }
}
