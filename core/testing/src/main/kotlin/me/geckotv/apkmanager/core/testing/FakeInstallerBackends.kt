package me.geckotv.apkmanager.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import me.geckotv.apkmanager.core.installer.InstallerBackends
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.InstallerBackend

/** [InstallerBackends] for tests; tests set [shizukuState] and [activeBackend] directly. */
class FakeInstallerBackends(
    shizukuState: ShizukuState = ShizukuState.NotInstalled,
    activeBackend: InstallerBackend = InstallerBackend.System,
) : InstallerBackends {
    override val shizukuState = MutableStateFlow(shizukuState)
    override val activeBackend = MutableStateFlow(activeBackend)

    var permissionRequests = 0
    var refreshes = 0

    override fun requestShizukuPermission() {
        permissionRequests++
    }

    override fun refreshShizukuState() {
        refreshes++
    }
}
