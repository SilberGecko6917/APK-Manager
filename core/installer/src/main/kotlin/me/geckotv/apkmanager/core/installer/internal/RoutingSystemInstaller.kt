package me.geckotv.apkmanager.core.installer.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import me.geckotv.apkmanager.core.installer.InstallerBackends
import me.geckotv.apkmanager.core.installer.ShizukuState
import me.geckotv.apkmanager.core.model.InstallerBackend
import java.io.File
import javax.inject.Inject
import javax.inject.Qualifier

/**
 * Installs through the backend chosen in the settings: Shizuku while it is ready, otherwise Android's
 * package installer.
 */
internal class RoutingSystemInstaller
    @Inject
    constructor(
        @Backend(InstallerBackend.System) private val system: SystemInstaller,
        @Backend(InstallerBackend.Shizuku) private val shizuku: SystemInstaller,
        private val backends: InstallerBackends,
        private val userSettingsRepository: UserSettingsRepository,
    ) : SystemInstaller {
        override fun canInstallPackages(): Boolean =
            backends.activeBackend.value == InstallerBackend.Shizuku || system.canInstallPackages()

        override fun install(
            file: File,
            packageName: String,
            label: String,
        ): Flow<SessionResult> = flow { emitAll(installer().install(file, packageName, label)) }

        private suspend fun installer(): SystemInstaller {
            if (userSettingsRepository.settings.first().installerBackend != InstallerBackend.Shizuku) return system
            // Right after the app starts (e.g. for automatic updates), Shizuku's binder may still be on its way.
            val ready =
                withTimeoutOrNull(SHIZUKU_WAIT_MILLIS) { backends.shizukuState.first { it is ShizukuState.Ready } }
            return if (ready != null) shizuku else system
        }

        private companion object {
            const val SHIZUKU_WAIT_MILLIS = 3_000L
        }
    }

/** Qualifies the [SystemInstaller] of one [InstallerBackend]. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class Backend(
    val value: InstallerBackend,
)
