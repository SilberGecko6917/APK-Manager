package me.geckotv.apkmanager.core.designsystem.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** A [HapticFeedback] that does nothing, used when the user turned haptic feedback off. */
internal object NoOpHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
