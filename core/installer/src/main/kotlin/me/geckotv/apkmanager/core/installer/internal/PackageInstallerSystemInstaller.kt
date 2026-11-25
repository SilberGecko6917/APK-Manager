package me.geckotv.apkmanager.core.installer.internal

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.installer.InstallFailure
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** [SystemInstaller] backed by [PackageInstaller] sessions. */
internal class PackageInstallerSystemInstaller
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @Dispatcher(ApkManagerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    ) : SystemInstaller {
        private val packageInstaller: PackageInstaller get() = context.packageManager.packageInstaller

        override fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

        override fun install(
            file: File,
            packageName: String,
            label: String,
        ): Flow<SessionResult> =
            flow {
                val sessionId =
                    try {
                        commit(file, packageName, label)
                    } catch (exception: IOException) {
                        emit(SessionResult.Failure(exception.toInstallFailure()))
                        return@flow
                    } catch (exception: SecurityException) {
                        emit(SessionResult.Failure(exception.toInstallFailure()))
                        return@flow
                    }
                var finished = false
                try {
                    emitAll(
                        InstallStatusReceiver.results
                            .map { it[sessionId] }
                            .filterNotNull()
                            .distinctUntilChanged()
                            .transformWhile { result ->
                                finished = result !is SessionResult.PendingUserAction
                                emit(result)
                                !finished
                            },
                    )
                } finally {
                    InstallStatusReceiver.consume(sessionId)
                    if (!finished) {
                        withContext(NonCancellable) { abandon(sessionId) }
                    }
                }
            }.flowOn(ioDispatcher)

        /** Writes [file] into a new session and commits it. Returns the session ID. */
        private fun commit(
            file: File,
            packageName: String,
            label: String,
        ): Int {
            val sessionId = packageInstaller.createSession(sessionParams(file, packageName, label))
            try {
                packageInstaller.openSession(sessionId).use { session ->
                    session.write(file)
                    session.commit(statusReceiver(sessionId).intentSender)
                }
            } catch (exception: IOException) {
                abandon(sessionId)
                throw exception
            } catch (exception: SecurityException) {
                abandon(sessionId)
                throw exception
            }
            return sessionId
        }

        private fun PackageInstaller.Session.write(file: File) {
            openWrite(APK_NAME, 0, file.length()).use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
                fsync(output)
            }
        }

        private fun Exception.toInstallFailure(): InstallFailure =
            if (this is IOException) InstallFailure.Storage else InstallFailure.SystemError(message)

        private fun sessionParams(
            file: File,
            packageName: String,
            label: String,
        ): PackageInstaller.SessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(packageName)
                setAppLabel(label)
                setSize(file.length())
                setInstallReason(PackageManager.INSTALL_REASON_USER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Updates of apps APK Manager installed need no confirmation; the system still asks
                    // for everything else.
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
                }
            }

        /**
         * The system fills the status into this intent, so it must be mutable; it is explicit, so only
         * [InstallStatusReceiver] receives it.
         */
        private fun statusReceiver(sessionId: Int): PendingIntent {
            val intent =
                Intent(context, InstallStatusReceiver::class.java)
                    .setAction(ACTION_INSTALL_STATUS)
                    .setPackage(context.packageName)
            val flags =
                PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            return PendingIntent.getBroadcast(context, sessionId, intent, flags)
        }

        private fun abandon(sessionId: Int) {
            try {
                packageInstaller.abandonSession(sessionId)
            } catch (_: SecurityException) {
                // The session is gone already.
            }
        }

        private companion object {
            const val APK_NAME = "base.apk"
            const val ACTION_INSTALL_STATUS = "me.geckotv.apkmanager.action.INSTALL_STATUS"
        }
    }
