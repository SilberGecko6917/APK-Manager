package me.geckotv.apkmanager.feature.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.geckotv.apkmanager.core.designsystem.icon.ApkManagerIcons
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.Version
import me.geckotv.apkmanager.core.model.versionFromTag
import me.geckotv.apkmanager.feature.apps.R

/** The most release tags of the history that the preview shows besides the latest release's. */
private const val PREVIEW_HISTORY_TAGS = 4

private val StatusIconSize = 20.dp

/** The tags to preview a pattern with: the latest release's, then up to four from the [history]. */
internal fun previewTags(
    latestRelease: Release?,
    history: List<Release>,
): List<String> =
    (listOfNotNull(latestRelease?.tagName) + history.map(Release::tagName))
        .distinct()
        .take(1 + PREVIEW_HISTORY_TAGS)

/**
 * The content of the sheet to edit the pattern that extracts the version from a release tag, with a
 * live preview of what it makes of [tags].
 *
 * @param pattern the saved pattern, which the field starts with.
 * @param onSaveClick called with the entered pattern; only possible while it is valid.
 */
@Composable
internal fun VersionPatternSheetContent(
    pattern: String?,
    tags: List<String>,
    onSaveClick: (String) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf(pattern.orEmpty()) }
    val isInvalid = text.isInvalidPattern()
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SheetTitle(text = stringResource(R.string.feature_apps_pattern_title))
        PatternField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.feature_apps_pattern_label),
            isError = isInvalid,
            imeAction = ImeAction.Done,
            hint = stringResource(R.string.feature_apps_pattern_hint),
        )
        TagPreview(tags = tags, pattern = text.takeUnless { isInvalid })
        OptionsSheetButtons(
            canReset = text.isNotEmpty(),
            canSave = !isInvalid,
            onResetClick = { text = "" },
            onCancelClick = onCancelClick,
            onSaveClick = { onSaveClick(text) },
        )
    }
}

/** A table of the [tags] and the version [pattern] extracts from each; without a pattern the tags stay as they are. */
@Composable
private fun TagPreview(
    tags: List<String>,
    pattern: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.feature_apps_pattern_preview),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            if (tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.feature_apps_pattern_preview_none),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TagRow(
                        tag = stringResource(R.string.feature_apps_pattern_tag_column),
                        version = stringResource(R.string.feature_apps_pattern_version_column),
                        isUnderstood = null,
                    )
                    tags.forEach { tag ->
                        val version = versionFromTag(tag, pattern)
                        TagRow(tag = tag, version = version, isUnderstood = Version.parse(version) != null)
                    }
                }
            }
        }
    }
}

/** A row of the tag table; the header row has no [isUnderstood]. */
@Composable
private fun TagRow(
    tag: String,
    version: String,
    isUnderstood: Boolean?,
    modifier: Modifier = Modifier,
) {
    val isHeader = isUnderstood == null
    val style =
        (if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium).copy(
            fontFamily = if (isHeader) FontFamily.Default else FontFamily.Monospace,
        )
    val color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tag,
            modifier = Modifier.weight(1f),
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = version,
            modifier = Modifier.weight(1f),
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when (isUnderstood) {
            null -> {
                Spacer(modifier = Modifier.size(StatusIconSize))
            }

            true -> {
                Icon(
                    painter = painterResource(ApkManagerIcons.Check),
                    contentDescription = stringResource(R.string.feature_apps_pattern_understood),
                    modifier = Modifier.size(StatusIconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            false -> {
                Icon(
                    painter = painterResource(ApkManagerIcons.Warning),
                    contentDescription = stringResource(R.string.feature_apps_pattern_not_understood),
                    modifier = Modifier.size(StatusIconSize),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
