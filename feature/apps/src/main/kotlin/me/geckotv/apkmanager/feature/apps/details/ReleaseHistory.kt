package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.component.SectionItemIcon
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.SourceError
import me.geckotv.apkmanager.feature.apps.InfoLabel
import me.geckotv.apkmanager.feature.apps.R
import me.geckotv.apkmanager.feature.apps.formatDate
import me.geckotv.apkmanager.feature.apps.lookUpMessages

private const val EXPANDED_CHEVRON_ROTATION = -90f
private const val COLLAPSED_CHEVRON_ROTATION = 90f

/** A release of the history at position [index] of [count] releases; the position decides the shape of its card. */
internal data class HistoryEntry(
    val release: Release,
    val index: Int,
    val count: Int,
)

/** A release in the history: its tag, name and date, and, once expanded, its notes. */
@Composable
internal fun ReleaseHistoryItem(
    entry: HistoryEntry,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInstalled: Boolean = false,
) {
    val release = entry.release
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ListItemDefaults.segmentedShapes(entry.index, entry.count).shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            ReleaseHistoryHeader(
                release = release,
                isExpanded = isExpanded,
                isInstalled = isInstalled,
                onClick = onClick,
            )
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    ReleaseNotesOrNone(notes = release.body)
                    TextButton(onClick = onOpenClick, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.feature_apps_details_open_github))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseHistoryHeader(
    release: Release,
    isExpanded: Boolean,
    isInstalled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) EXPANDED_CHEVRON_ROTATION else COLLAPSED_CHEVRON_ROTATION,
        label = "chevron",
    )
    Row(
        modifier =
            modifier
                .clickable(
                    onClickLabel =
                        stringResource(
                            if (isExpanded) {
                                R.string.feature_apps_details_release_collapse
                            } else {
                                R.string.feature_apps_details_release_expand
                            },
                        ),
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = release.tagName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            release.summary()?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isInstalled) InfoLabel(text = stringResource(R.string.feature_apps_details_installed_label))
        if (release.isPrerelease) InfoLabel(text = stringResource(R.string.feature_apps_pre_release))
        Icon(
            painter = painterResource(ApkManagerIcons.ChevronRight),
            contentDescription = null,
            modifier = Modifier.rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The release name, if it says more than the tag, and the date it was published. */
@Composable
private fun Release.summary(): String? {
    val title = name?.takeIf { it.isNotBlank() && it != tagName }
    return listOfNotNull(title, publishedAt?.let(::formatDate))
        .joinToString(separator = " · ")
        .takeIf { it.isNotEmpty() }
}

/** The [notes] of a release, or a hint that there are none. */
@Composable
internal fun ReleaseNotesOrNone(
    notes: String?,
    modifier: Modifier = Modifier,
) {
    if (notes.isNullOrBlank()) {
        Text(
            text = stringResource(R.string.feature_apps_details_notes_none),
            modifier = modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        ReleaseNotes(markdown = notes, modifier = modifier)
    }
}

/**
 * The release history is loading. Without [showProgressIndicator], e.g. in screenshot tests, a static
 * icon stands in for the indicator, which animates forever.
 */
@Composable
internal fun ReleaseHistoryLoading(
    showProgressIndicator: Boolean,
    modifier: Modifier = Modifier,
) {
    HistoryMessageCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showProgressIndicator) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp).padding(8.dp), strokeWidth = 3.dp)
            } else {
                SectionItemIcon(ApkManagerIcons.Schedule)
            }
            Text(
                text = stringResource(R.string.feature_apps_details_history_loading),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** The release history could not be loaded because of [error]. */
@Composable
internal fun ReleaseHistoryFailed(
    error: SourceError,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryMessageCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionItemIcon(ApkManagerIcons.Warning)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                error.lookUpMessages().forEachIndexed { index, message ->
                    Text(
                        text = message,
                        style =
                            if (index ==
                                0
                            ) {
                                MaterialTheme.typography.bodyLarge
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                        color =
                            if (index ==
                                0
                            ) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
        TextButton(onClick = onRetryClick, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.feature_apps_error_retry))
        }
    }
}

/** The history has no releases, which only happens if the repository has none that are published. */
@Composable
internal fun ReleaseHistoryEmpty(modifier: Modifier = Modifier) {
    HistoryMessageCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.feature_apps_details_history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryMessageCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ListItemDefaults.segmentedShapes(0, 1).shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
