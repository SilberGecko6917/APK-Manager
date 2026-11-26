package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import me.geckotv.apkmanager.feature.apps.R

/** Notes that are longer than this many lines are collapsed. */
private const val COLLAPSED_LINES = 12

private val FadeHeight = 48.dp

/**
 * Release notes in Markdown, styled with the Material 3 typography. The headings are small, because
 * the notes sit inside cards rather than on a page of their own.
 */
@Composable
internal fun ReleaseNotes(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val code = typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    Markdown(
        markdownState = rememberMarkdownState(content = markdown.trim(), immediate = true),
        colors =
            markdownColor(
                text = colorScheme.onSurface,
                codeBackground = colorScheme.surfaceContainerHighest,
                inlineCodeBackground = colorScheme.surfaceContainerHighest,
                dividerColor = colorScheme.outlineVariant,
                tableBackground = colorScheme.surfaceContainerHigh,
            ),
        typography =
            markdownTypography(
                h1 = typography.titleLarge,
                h2 = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                h3 = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                h4 = typography.titleSmall,
                h5 = typography.labelLarge,
                h6 = typography.labelMedium,
                text = typography.bodyMedium,
                code = code,
                inlineCode = code,
                quote = typography.bodyMedium,
                paragraph = typography.bodyMedium,
                ordered = typography.bodyMedium,
                bullet = typography.bodyMedium,
                list = typography.bodyMedium,
                table = typography.bodyMedium,
            ),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * [ReleaseNotes] that are cut off after about 12 lines, with a fade and a button to show all of them. Short
 * notes are shown completely, without the button.
 *
 * @param containerColor the color behind the notes, which the fade blends into.
 */
@Composable
internal fun CollapsibleReleaseNotes(
    markdown: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(markdown) { mutableStateOf(false) }
    var fullHeightPx by remember(markdown) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val collapsedHeight: Dp =
        with(density) {
            MaterialTheme.typography.bodyMedium.lineHeight
                .toDp()
        } * COLLAPSED_LINES
    val collapsedHeightPx = with(density) { collapsedHeight.roundToPx() }
    // Cutting off a few pixels is not worth a button.
    val isCollapsible = fullHeightPx > collapsedHeightPx + with(density) { FadeHeight.roundToPx() }
    val showsAll = isExpanded || !isCollapsible

    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .animateContentSize()
                    .fillMaxWidth()
                    .heightIn(max = if (showsAll) Dp.Infinity else collapsedHeight)
                    .clipToBounds(),
        ) {
            // The notes are measured at full height, so their size is known even while they are cut off.
            ReleaseNotes(
                markdown = markdown,
                modifier =
                    Modifier
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .onSizeChanged { fullHeightPx = it.height },
            )
            if (!showsAll) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(FadeHeight)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, containerColor))),
                )
            }
        }
        if (isCollapsible) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text =
                        stringResource(
                            if (isExpanded) {
                                R.string.feature_apps_details_notes_show_less
                            } else {
                                R.string.feature_apps_details_notes_show_more
                            },
                        ),
                )
            }
        }
    }
}
