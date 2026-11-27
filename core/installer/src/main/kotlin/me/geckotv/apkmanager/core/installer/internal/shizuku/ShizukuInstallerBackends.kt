package me.geckotv.apkmanager.core.installer.internal.shizuku

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.geckotv.apkmanager.core.common.ApplicationScope
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.installer.InstallerBackends
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.InstallerBackend
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

/** [InstallerBackends] that follows Shizuku's binder and permission through its listeners. */
@Singleton
internal class ShizukuInstallerBackends
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        userSettingsRepository: UserSettingsRepository,
        @ApplicationScope scope: CoroutineScope,
    ) : InstallerBackends {
        private val state = MutableStateFlow(currentState())

        override val shizukuState: StateFlow<ShizukuState> = state.asStateFlow()

        override val activeBackend: StateFlow<InstallerBackend> =
            combine(userSettingsRepository.settings, state) { settings, shizuku ->
                if (settings.installerBackend == InstallerBackend.Shizuku && shizuku is ShizukuState.Ready) {
                    InstallerBackend.Shizuku
                } else {
                    InstallerBackend.System
                }
            }.stateIn(scope, SharingStarted.Eagerly, InstallerBackend.System)

        init {
            Shizuku.addBinderReceivedListenerSticky { refreshShizukuState() }
            Shizuku.addBinderDeadListener { refreshShizukuState() }
            Shizuku.addRequestPermissionResultListener { _, _ -> refreshShizukuState() }
        }

        override fun requestShizukuPermission() {
            if (state.value == ShizukuState.PermissionRequired) {
                try {
                    Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                } catch (_: IllegalStateException) {
                    // The binder died in the meantime.
                    refreshShizukuState()
                }
            }
        }

        override fun refreshShizukuState() {
            state.value = currentState()
        }

        private fun currentState(): ShizukuState =
            try {
                when {
                    !Shizuku.pingBinder() -> {
                        if (isShizukuInstalled()) ShizukuState.NotRunning else ShizukuState.NotInstalled
                    }

                    Shizuku.isPreV11() -> {
                        ShizukuState.Unsupported
                    }

                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                        ShizukuState.Ready(isRoot = Shizuku.getUid() == ROOT_UID)
                    }

                    Shizuku.shouldShowRequestPermissionRationale() -> {
                        ShizukuState.PermissionDenied
                    }

                    else -> {
                        ShizukuState.PermissionRequired
                    }
                }
            } catch (_: IllegalStateException) {
                // Calls fail while the binder is dead.
                ShizukuState.NotRunning
            }

        private fun isShizukuInstalled(): Boolean =
            try {
                context.packageManager.getApplicationInfo(SHIZUKU_PACKAGE, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }

        private companion object {
            const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
            const val PERMISSION_REQUEST_CODE = 1
            const val ROOT_UID = 0
        }
    }
