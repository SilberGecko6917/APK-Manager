package me.geckotv.apkmanager.core.installer

import kotlinx.coroutines.flow.StateFlow
import me.geckotv.apkmanager.core.model.TrackedApp

/**
 * Downloads, verifies and installs the latest release of tracked apps. Installs keep running when the
 * screen that started them is left.
 */
interface AppInstaller {
    /** The state of every install that is running or finished and not yet dismissed, by app ID. */
    val installs: StateFlow<Map<Long, InstallState>>

    /** Whether APK Manager may install apps ("Install unknown apps" is allowed). */
    fun canInstallPackages(): Boolean

    /** Installs the latest release of [app]. Does nothing if an install of [app] is already running. */
    fun install(app: TrackedApp)

    /** Cancels a running install of app [appId]. */
    fun cancel(appId: Long)

    /** The UI launched the confirmation of [InstallState.WaitingForUser] for app [appId]. */
    fun onConfirmationShown(appId: Long)

    /** The user wants to see the confirmation for app [appId] again, e.g. after closing it without an answer. */
    fun showConfirmationAgain(appId: Long)

    /** Forgets the finished install of app [appId]. */
    fun dismiss(appId: Long)
}
