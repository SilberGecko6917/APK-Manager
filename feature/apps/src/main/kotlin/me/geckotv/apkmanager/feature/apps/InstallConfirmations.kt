package me.geckotv.apkmanager.feature.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallState
import javax.inject.Inject

/**
 * Launches the system dialog that confirms an install while the app is in the foreground, whatever
 * screen is shown, and reports it so it is launched only once. If several apps wait, the next dialog
 * follows only after the user is back in the app, so dialogs never stack. Add it once to the app,
 * outside the screens.
 */
@Composable
fun InstallConfirmations(viewModel: InstallConfirmationsViewModel = hiltViewModel()) {
    val confirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    confirmation?.let {
        ConfirmationLaunchEffect(
            confirmation = it,
            onShow = viewModel::onConfirmationShown,
            onFail = viewModel::onConfirmationFailed,
        )
    }
}

@Composable
private fun ConfirmationLaunchEffect(
    confirmation: PendingConfirmation,
    onShow: (Long) -> Unit,
    onFail: (Long) -> Unit,
) {
    val context = LocalContext.current
    val currentOnShow by rememberUpdatedState(onShow)
    val currentOnFail by rememberUpdatedState(onFail)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    var launchedSinceResume by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { launchedSinceResume = false }
    LaunchedEffect(confirmation, lifecycleState, launchedSinceResume) {
        if (launchedSinceResume || !lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
        launchedSinceResume = true
        try {
            context.startActivity(confirmation.intent)
            currentOnShow(confirmation.appId)
        } catch (_: ActivityNotFoundException) {
            currentOnFail(confirmation.appId)
        }
    }
}

/** The system dialog to launch so the user confirms the install of app [appId]. */
data class PendingConfirmation(
    val appId: Long,
    val intent: Intent,
)

/** Tells [InstallConfirmations] which install confirmation to show, and forwards what happened to the installer. */
@HiltViewModel
class InstallConfirmationsViewModel
    @Inject
    constructor(
        private val appInstaller: AppInstaller,
    ) : ViewModel() {
        /** The first install that waits for the user to confirm it in the system dialog. */
        val pendingConfirmation: StateFlow<PendingConfirmation?> =
            appInstaller.installs
                .map { installs ->
                    installs.firstNotNullOfOrNull { (appId, state) ->
                        (state as? InstallState.WaitingForUser)
                            ?.takeUnless { it.isShown }
                            ?.let { PendingConfirmation(appId, it.intent) }
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = null,
                )

        /** The system confirmation of the install of app [appId] is on screen. */
        fun onConfirmationShown(appId: Long) {
            appInstaller.onConfirmationShown(appId)
        }

        /** The system confirmation of the install of app [appId] could not be shown; cancels the install. */
        fun onConfirmationFailed(appId: Long) {
            appInstaller.cancel(appId)
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
