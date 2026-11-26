package me.geckotv.apkmanager.core.installer.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import me.geckotv.apkmanager.core.installer.InstallState
import me.geckotv.apkmanager.core.installer.R
import javax.inject.Inject

/** Shows the progress of one app's install as a notification. */
internal interface InstallNotifier {
    fun show(
        appId: Long,
        name: String,
        state: InstallState,
    )

    fun cancel(appId: Long)

    /** The notification of the foreground service, summarizing [count] running installs. */
    fun summary(count: Int): Notification
}

internal class AndroidInstallNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InstallNotifier {
        private val notificationManager = NotificationManagerCompat.from(context)

        @SuppressLint("MissingPermission") // Checked by canNotify().
        override fun show(
            appId: Long,
            name: String,
            state: InstallState,
        ) {
            if (!canNotify()) return
            createChannel()
            notificationManager.notify(notificationId(appId), notification(appId, name, state))
        }

        override fun cancel(appId: Long) {
            notificationManager.cancel(notificationId(appId))
        }

        override fun summary(count: Int): Notification {
            createChannel()
            return builder()
                .setContentTitle(context.resources.getQuantityString(R.plurals.core_installer_summary, count, count))
                .setGroupSummary(true)
                .setOngoing(true)
                .build()
        }

        private fun notification(
            appId: Long,
            name: String,
            state: InstallState,
        ): Notification =
            when (state) {
                is InstallState.Downloading -> downloading(appId, name, state)
                InstallState.Verifying -> working(appId, context.getString(R.string.core_installer_verifying, name))
                InstallState.Installing -> working(appId, context.getString(R.string.core_installer_installing, name))
                is InstallState.WaitingForUser -> waiting(name)
                is InstallState.Succeeded, is InstallState.Failed -> working(appId, name)
            }

        private fun downloading(
            appId: Long,
            name: String,
            state: InstallState.Downloading,
        ): Notification {
            val progress = state.progress
            val total = state.totalBytes
            val text =
                if (progress != null && total != null) {
                    context.getString(
                        R.string.core_installer_downloading_progress,
                        (progress * PERCENT).toInt(),
                        Formatter.formatShortFileSize(context, state.bytesRead),
                        Formatter.formatShortFileSize(context, total),
                    )
                } else {
                    Formatter.formatShortFileSize(context, state.bytesRead)
                }
            return builder()
                .setContentTitle(context.getString(R.string.core_installer_downloading, name))
                .setContentText(text)
                .setProgress(PERCENT, ((progress ?: 0f) * PERCENT).toInt(), progress == null)
                .setOngoing(true)
                .addAction(0, context.getString(R.string.core_installer_cancel), cancelIntent(appId))
                .build()
        }

        private fun working(
            appId: Long,
            title: String,
        ): Notification =
            builder()
                .setContentTitle(title)
                .setProgress(0, 0, true)
                .setOngoing(true)
                .addAction(0, context.getString(R.string.core_installer_cancel), cancelIntent(appId))
                .build()

        private fun waiting(name: String): Notification =
            builder()
                .setContentTitle(context.getString(R.string.core_installer_waiting, name))
                .setContentText(context.getString(R.string.core_installer_waiting_text))
                .setAutoCancel(true)
                .build()

        private fun builder(): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.core_installer_ic_download)
                .setContentIntent(openAppIntent())
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setGroup(GROUP_KEY)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        private fun cancelIntent(appId: Long): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                appId.toInt(),
                Intent(context, InstallActionReceiver::class.java)
                    .setAction(InstallActionReceiver.ACTION_CANCEL)
                    .putExtra(InstallActionReceiver.EXTRA_APP_ID, appId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun openAppIntent(): PendingIntent? {
            val intent =
                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    ?: return null
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun canNotify(): Boolean {
            val permitted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            return permitted && notificationManager.areNotificationsEnabled()
        }

        private fun createChannel() {
            notificationManager.createNotificationChannel(
                NotificationChannelCompat
                    .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.core_installer_channel_installs))
                    .setDescription(context.getString(R.string.core_installer_channel_installs_description))
                    .build(),
            )
        }

        private fun notificationId(appId: Long): Int = (appId % Int.MAX_VALUE).toInt() + APP_ID_OFFSET

        companion object {
            const val CHANNEL_ID = "installs"
            const val GROUP_KEY = "me.geckotv.apkmanager.INSTALLS"

            /** ID of the foreground service notification, which also summarizes the group. */
            const val SUMMARY_ID = 2
            private const val APP_ID_OFFSET = 100_000
            private const val PERCENT = 100
        }
    }
