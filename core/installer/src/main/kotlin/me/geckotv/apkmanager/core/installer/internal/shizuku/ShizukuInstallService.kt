package me.geckotv.apkmanager.core.installer.internal.shizuku

import android.content.Context
import androidx.annotation.Keep
import java.io.IOException
import kotlin.system.exitProcess

/**
 * Runs in a process that Shizuku starts with ADB (shell) or root privileges, see
 * [IShizukuInstallService]. Shizuku creates it by class name, so its constructors are kept.
 */
internal class ShizukuInstallService
    @Keep
    constructor() : IShizukuInstallService.Stub() {
        /** Shizuku 13+ prefers this constructor. */
        @Keep
        @Suppress("UNUSED_PARAMETER")
        constructor(context: Context) : this()

        private var process: Process? = null

        override fun destroy() {
            abort()
            exitProcess(0)
        }

        override fun begin(
            size: Long,
            installerPackageName: String,
        ) {
            abort()
            process =
                ProcessBuilder(
                    PM,
                    "install",
                    "-r",
                    "-S",
                    size.toString(),
                    "-i",
                    installerPackageName,
                    "--user",
                    "current",
                ).redirectErrorStream(true)
                    .start()
        }

        override fun write(chunk: ByteArray) {
            try {
                checkNotNull(process) { "begin() was not called" }.outputStream.write(chunk)
            } catch (_: IOException) {
                // pm stopped reading, e.g. because the APK is invalid; finish() returns its output.
            }
        }

        override fun finish(): String {
            val running = checkNotNull(process) { "begin() was not called" }
            process = null
            try {
                running.outputStream.close()
            } catch (_: IOException) {
                // pm stopped reading already.
            }
            val output = running.inputStream.bufferedReader().use { it.readText() }
            val exitCode = running.waitFor()
            return output.ifBlank { "Failure [exit code $exitCode]" }
        }

        override fun abort() {
            process?.destroy()
            process = null
        }

        private companion object {
            /** Absolute, because the user service process may not have a usable PATH. */
            const val PM = "/system/bin/pm"
        }
    }
