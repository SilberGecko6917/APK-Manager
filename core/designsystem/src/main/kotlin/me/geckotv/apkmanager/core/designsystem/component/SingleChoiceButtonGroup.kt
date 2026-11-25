package me.geckotv.apkmanager.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** An option of a [SingleChoiceButtonGroup]. */
@Immutable
data class ButtonGroupOption(
    val label: String,
    @param:DrawableRes val icon: Int? = null,
)

/**
 * A connected button group where exactly one option is selected, e.g. for choosing the theme mode.
 * The selected button morphs into a fully rounded shape.
 */
@Composable
fun SingleChoiceButtonGroup(
    options: List<ButtonGroupOption>,
    selectedIndex: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = index == selectedIndex,
                onCheckedChange = {
                    if (index != selectedIndex) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onOptionSelect(index)
                    }
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                if (option.icon != null) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = option.label,
                    modifier = Modifier.padding(start = if (option.icon != null) 8.dp else 0.dp),
                    maxLines = 1,
                )
            }
        }
    }
}
