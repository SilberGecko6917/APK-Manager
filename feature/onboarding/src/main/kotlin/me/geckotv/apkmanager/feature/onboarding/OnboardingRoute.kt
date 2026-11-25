package me.geckotv.apkmanager.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Stateful entry point of the onboarding: tracks the permission state and starts the requests. */
@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var installGranted by remember { mutableStateOf(context.packageManager.canRequestPackageInstalls()) }
    var notificationsGranted by remember { mutableStateOf(context.notificationPermissionState()) }

    // The user returns from the system settings, so check again whenever the screen resumes.
    LifecycleResumeEffect(Unit) {
        installGranted = context.packageManager.canRequestPackageInstalls()
        notificationsGranted = context.notificationPermissionState()
        onPauseOrDispose {}
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsGranted = granted
        }

    OnboardingScreen(
        installPermissionGranted = installGranted,
        notificationPermissionGranted = notificationsGranted,
        onRequestInstallPermission = {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${context.packageName}".toUri(),
                ),
            )
        },
        onRequestNotificationPermission = {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onFinish = viewModel::completeOnboarding,
        modifier = modifier,
    )
}

/** `null` when notifications need no runtime permission (below Android 13). */
private fun Context.notificationPermissionState(): Boolean? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        null
    }
