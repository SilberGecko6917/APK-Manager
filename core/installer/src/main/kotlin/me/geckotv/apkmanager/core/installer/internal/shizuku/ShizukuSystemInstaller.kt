package me.geckotv.apkmanager.core.installer.internal.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.installer.InstallFailure
import me.geckotv.apkmanager.core.installer.internal.SessionResult
import me.geckotv.apkmanager.core.installer.internal.SystemInstaller
import rikka.shizuku.Shizuku
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Installs APKs with `pm install` in a Shizuku user service ([ShizukuInstallService]), so Android
 * does not ask the user. APK Manager stays the installer of record. Installs run one at a time.
 */
@Singleton
internal class ShizukuSystemInstaller
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @Dispatcher(ApkManagerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : SystemInstaller {
        private val mutex = Mutex()

        override fun canInstallPackages(): Boolean = true

        override fun install(
            file: File,
            packageName: String,
            label: String,
        ): Flow<SessionResult> =
            flow {
                val result =
                    try {
                        mutex.withLock { withService { service -> stream(service, file) } }
                    } catch (exception: RemoteException) {
                        SessionResult.Failure(InstallFailure.SystemError(exception.message))
                    } catch (exception: IllegalStateException) {
                        // Shizuku stopped, or its binder is gone.
                        SessionResult.Failure(InstallFailure.SystemError(exception.message))
                    } catch (_: TimeoutCancellationException) {
                        SessionResult.Failure(InstallFailure.SystemError("Shizuku did not start the installer"))
                    }
                emit(result)
            }.flowOn(ioDispatcher)

        private fun stream(
            service: IShizukuInstallService,
            file: File,
        ): SessionResult {
            var finished = false
            try {
                service.begin(file.length(), context.packageName)
                file.inputStream().use { input -> sendChunks(service, input) }
                val output = service.finish()
                finished = true
                return PmInstallOutput.toSessionResult(output)
            } finally {
                if (!finished) runCatching { service.abort() }
            }
        }

        private fun sendChunks(
            service: IShizukuInstallService,
            input: InputStream,
        ) {
            val buffer = ByteArray(CHUNK_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                service.write(if (read == buffer.size) buffer else buffer.copyOf(read))
                read = input.read(buffer)
            }
        }

        /** Starts the user service, runs [block] with it and stops it again. */
        private suspend fun <T> withService(block: suspend (IShizukuInstallService) -> T): T {
            val args =
                Shizuku
                    .UserServiceArgs(ComponentName(context.packageName, ShizukuInstallService::class.java.name))
                    .daemon(false)
                    .tag(SERVICE_TAG)
                    .processNameSuffix(SERVICE_PROCESS_SUFFIX)
                    .version(SERVICE_VERSION)
            val connection = ServiceConnectionAwaiter()
            try {
                val service = withTimeout(BIND_TIMEOUT_MILLIS) { connection.bind(args) }
                return block(service)
            } finally {
                withContext(NonCancellable) {
                    runCatching { Shizuku.unbindUserService(args, connection, true) }
                }
            }
        }

        /** Waits for Shizuku to connect the user service. */
        private class ServiceConnectionAwaiter : ServiceConnection {
            private var onConnected: ((IShizukuInstallService) -> Unit)? = null

            suspend fun bind(args: Shizuku.UserServiceArgs): IShizukuInstallService =
                suspendCancellableCoroutine { continuation ->
                    onConnected = { service -> if (continuation.isActive) continuation.resume(service) }
                    Shizuku.bindUserService(args, this)
                }

            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                if (binder != null && binder.pingBinder()) {
                    onConnected?.invoke(IShizukuInstallService.Stub.asInterface(binder))
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }

        private companion object {
            /** Binder transactions must stay well below 1 MB. */
            const val CHUNK_SIZE = 256 * 1024
            const val BIND_TIMEOUT_MILLIS = 15_000L
            const val SERVICE_TAG = "installer"
            const val SERVICE_PROCESS_SUFFIX = "installer"

            /** Raise when [IShizukuInstallService] changes, so Shizuku starts the new service. */
            const val SERVICE_VERSION = 1
        }
    }
