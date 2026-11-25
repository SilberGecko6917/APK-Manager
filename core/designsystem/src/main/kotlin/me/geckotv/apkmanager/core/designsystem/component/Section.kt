package me.geckotv.apkmanager.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shapes for the [SectionItem] currently being composed, provided by [Section] and [section]. */
private val LocalSectionItemShapes = compositionLocalOf<ListItemShapes?> { null }

/** Receiver scope of [Section]. */
interface SectionScope {
    /** Adds an item, usually a [SectionItem] or [SwitchSectionItem]. */
    fun item(content: @Composable () -> Unit)
}

private class SectionScopeImpl : SectionScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items += content
    }
}

/**
 * A group of related items shown as a segmented list: the group has large rounded outer corners,
 * items inside it are separated by small gaps and have small inner corners.
 *
 * ```
 * Section(title = "Appearance") {
 *     item { SwitchSectionItem(headline = "Dynamic color", checked = dynamic, onCheckedChange = { … }) }
 *     item { SectionItem(headline = "Theme", supportingText = "System default", onClick = { … }) }
 * }
 * ```
 *
 * For long lists inside a `LazyColumn`, use [section] instead.
 */
@Composable
fun Section(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: SectionScope.() -> Unit,
) {
    val items = SectionScopeImpl().apply(content).items
    Column(modifier = modifier) {
        if (title != null) {
            SectionHeader(text = title)
        }
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            items.forEachIndexed { index, item ->
                CompositionLocalProvider(
                    LocalSectionItemShapes provides ListItemDefaults.segmentedShapes(index, items.size),
                    content = item,
                )
            }
        }
    }
}

/**
 * Lazy variant of [Section]: adds an optional [title], with an optional [headerAction] at its end,
 * and one segmented item per element of [items] to a `LazyColumn`.
 */
fun <T> LazyListScope.section(
    items: List<T>,
    title: String? = null,
    key: ((T) -> Any)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    if (title != null) {
        item(contentType = "SectionHeader") { SectionHeader(text = title, action = headerAction) }
    }
    itemsIndexed(
        items = items,
        key = key?.let { itemKey -> { _, item -> itemKey(item) } },
        contentType = { _, _ -> "SectionItem" },
    ) { index, item ->
        CompositionLocalProvider(
            LocalSectionItemShapes provides ListItemDefaults.segmentedShapes(index, items.size),
        ) {
            Box(modifier = Modifier.padding(top = if (index > 0) ListItemDefaults.SegmentedGap else 0.dp)) {
                itemContent(item)
            }
        }
    }
}

/** Title above a [Section], with an optional [action] at its end, e.g. a `TextButton`. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    if (action == null) {
        Text(
            text = text,
            modifier =
                modifier
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
    } else {
        Row(
            modifier = modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
            action()
        }
    }
}

/** Default values for section items. */
object SectionDefaults {
    /**
     * Item colors: items use a container color that stands out from the screen background. Checked
     * items (e.g. an enabled [SwitchSectionItem]) keep the same colors, so a group of switches
     * stays calm; the switch itself shows the state.
     */
    @Composable
    fun colors(): ListItemColors {
        val colorScheme = MaterialTheme.colorScheme
        return ListItemDefaults.segmentedColors(
            containerColor = colorScheme.surfaceContainer,
            contentColor = colorScheme.onSurface,
            leadingContentColor = colorScheme.onSurfaceVariant,
            trailingContentColor = colorScheme.onSurfaceVariant,
            supportingContentColor = colorScheme.onSurfaceVariant,
            selectedContainerColor = colorScheme.surfaceContainer,
            selectedContentColor = colorScheme.onSurface,
            selectedLeadingContentColor = colorScheme.onSurfaceVariant,
            selectedTrailingContentColor = colorScheme.onSurfaceVariant,
            selectedSupportingContentColor = colorScheme.onSurfaceVariant,
        )
    }

    @Composable
    internal fun shapes(): ListItemShapes = LocalSectionItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
}

/**
 * A single row of a [Section].
 *
 * @param headline the main text.
 * @param supportingText optional secondary text below the headline.
 * @param leadingContent optional content before the text, usually a [SectionItemIcon].
 * @param trailingContent optional content after the text, e.g. a value or a chevron.
 * @param onClick called when the item is clicked; `null` makes the item non-interactive.
 */
@Composable
fun SectionItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val supporting: (@Composable () -> Unit)? = supportingText?.let { { Text(it) } }
    if (onClick != null) {
        val haptics = LocalHapticFeedback.current
        SegmentedListItem(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            },
            shapes = SectionDefaults.shapes(),
            modifier = modifier,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            supportingContent = supporting,
            colors = SectionDefaults.colors(),
        ) {
            Text(headline)
        }
    } else {
        SegmentedListItem(
            shapes = SectionDefaults.shapes(),
            modifier = modifier,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            supportingContent = supporting,
            colors = SectionDefaults.colors(),
        ) {
            Text(headline)
        }
    }
}

/** A [SectionItem] with a switch. The whole row toggles [checked]. */
@Composable
fun SwitchSectionItem(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    SegmentedListItem(
        checked = checked,
        onCheckedChange = { value ->
            haptics.performHapticFeedback(if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onCheckedChange(value)
        },
        shapes = SectionDefaults.shapes(),
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        supportingContent = supportingText?.let { { Text(it) } },
        colors = SectionDefaults.colors(),
    ) {
        Text(headline)
    }
}

/** An icon in a tinted circle, for the leading slot of section items. */
@Composable
fun SectionItemIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
