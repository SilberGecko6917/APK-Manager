package me.geckotv.apkmanager.feature.apps

import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.ApkAssets
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus

/**
 * A tracked [app] in the list and on its details screen.
 *
 * @property install the running install of the app, `null` if none is running.
 * @property hasCompatibleApk whether the latest release has an APK for this device.
 */
data class AppItem(
    val app: TrackedApp,
    val install: InstallState? = null,
    val hasCompatibleApk: Boolean = false,
) {
    val id: Long get() = app.id

    /** The install button to offer, `null` if none fits or an install is running. */
    val installAction: InstallAction?
        get() =
            when {
                install is InstallState.WaitingForUser && install.isShown -> InstallAction.Confirm
                install != null -> null
                app.updateStatus == UpdateStatus.UpdateAvailable -> InstallAction.Update
                app.updateStatus == UpdateStatus.NotInstalled && hasCompatibleApk -> InstallAction.Install
                else -> null
            }
}

/** What installing the latest release does to an app. */
enum class InstallAction {
    Install,
    Update,

    /** Show the system confirmation of a running install again. */
    Confirm,
}

/** The item of this app, with its running install and whether the latest release fits a device with [supportedAbis]. */
internal fun TrackedApp.toItem(
    installs: Map<Long, InstallState>,
    supportedAbis: List<String>,
): AppItem =
    AppItem(
        app = this,
        install = installs[id]?.takeIf { it.isActive },
        hasCompatibleApk =
            latestRelease?.let { ApkAssets.select(it.assets, supportedAbis, preferredAssetName) } != null,
    )
