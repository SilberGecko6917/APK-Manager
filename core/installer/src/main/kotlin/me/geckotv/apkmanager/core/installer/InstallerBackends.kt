package me.geckotv.apkmanager.core.installer

import kotlinx.coroutines.flow.StateFlow
import me.geckotv.apkmanager.core.model.InstallerBackend

/** Which [InstallerBackend] installs APKs, and whether Shizuku can be used. */
interface InstallerBackends {
    /** Whether Shizuku is installed, running and allowed. */
    val shizukuState: StateFlow<ShizukuState>

    /**
     * The backend that installs APKs now: [InstallerBackend.Shizuku] while it is selected in the
     * settings and [ShizukuState.Ready], otherwise [InstallerBackend.System].
     */
    val activeBackend: StateFlow<InstallerBackend>

    /** Asks Shizuku for permission. The answer arrives in [shizukuState]. */
    fun requestShizukuPermission()

    /** Checks Shizuku again, e.g. when the user comes back from the Shizuku app. */
    fun refreshShizukuState()
}

/** Whether APK Manager can install through [Shizuku](https://shizuku.rikka.app/) (or Sui). */
sealed interface ShizukuState {
    /** The Shizuku app is not installed (and Sui is not active). */
    data object NotInstalled : ShizukuState

    /** Shizuku is installed but not started. */
    data object NotRunning : ShizukuState

    /** The installed Shizuku is too old (before version 11). */
    data object Unsupported : ShizukuState

    /** Shizuku runs; APK Manager has to ask for permission. */
    data object PermissionRequired : ShizukuState

    /** The user denied the permission for good; it can be allowed again in the Shizuku app. */
    data object PermissionDenied : ShizukuState

    /** Shizuku runs and APK Manager may use it, with root ([isRoot]) or ADB privileges. */
    data class Ready(
        val isRoot: Boolean,
    ) : ShizukuState
}
