package me.geckotv.apkmanager.core.installer.internal

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Starts and stops the [InstallService]. */
internal interface InstallServiceController {
    /** Runs the service, or updates its notification to [count] running installs. */
    fun start(count: Int)

    fun stop()
}

internal class AndroidInstallServiceController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InstallServiceController {
        override fun start(count: Int) {
            val intent = Intent(context, InstallService::class.java).putExtra(InstallService.EXTRA_COUNT, count)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: IllegalStateException) {
                // Android does not allow starting a foreground service from the background; the install
                // keeps running without it, as long as the process lives.
            }
        }

        override fun stop() {
            context.stopService(Intent(context, InstallService::class.java))
        }
    }

/**
 * Foreground service that keeps the process alive while installs are running. It does no work itself;
 * the installs run in the application scope.
 */
@AndroidEntryPoint
internal class InstallService : Service() {
    @Inject
    lateinit var notifier: InstallNotifier

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val count = intent?.getIntExtra(EXTRA_COUNT, 1) ?: 1
        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, AndroidInstallNotifier.SUMMARY_ID, notifier.summary(count), type)
        return START_NOT_STICKY
    }

    /** Android 15+ limits data sync services; the installs go on without the service then. */
    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        stopSelf()
    }

    companion object {
        const val EXTRA_COUNT = "count"
    }
}
