package me.geckotv.apkmanager.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import me.geckotv.apkmanager.core.installer.AppInstaller
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.model.TrackedApp

/**
 * [AppInstaller] for tests: [install] records the app and sets [stateAfterInstall]; tests drive the
 * state further with [setState].
 */
class FakeAppInstaller(
    var canInstall: Boolean = true,
    var stateAfterInstall: InstallState = InstallState.Downloading(bytesRead = 0, totalBytes = null),
) : AppInstaller {
    private val state = MutableStateFlow<Map<Long, InstallState>>(emptyMap())

    override val installs: StateFlow<Map<Long, InstallState>> = state

    val installedApps = mutableListOf<TrackedApp>()
    val confirmationsShown = mutableListOf<Long>()

    fun setState(
        appId: Long,
        installState: InstallState?,
    ) {
        state.update { if (installState == null) it - appId else it + (appId to installState) }
    }

    override fun canInstallPackages(): Boolean = canInstall

    override fun install(app: TrackedApp) {
        if (state.value[app.id]?.isActive == true) return
        installedApps += app
        setState(app.id, stateAfterInstall)
    }

    override fun cancel(appId: Long) {
        setState(appId, null)
    }

    override fun onConfirmationShown(appId: Long) {
        confirmationsShown += appId
        if (state.value[appId] is InstallState.WaitingForUser) setState(appId, InstallState.Installing)
    }

    override fun dismiss(appId: Long) {
        if (state.value[appId]?.isActive != true) setState(appId, null)
    }
}
