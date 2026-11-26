package me.geckotv.apkmanager.core.work.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import me.geckotv.apkmanager.core.data.repository.TrackedAppsRepository
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdateStatus
import me.geckotv.apkmanager.core.work.R
import me.geckotv.apkmanager.core.work.UpdateNotifier
import javax.inject.Inject

/** [UpdateNotifier] that posts one notification per app, grouped under a summary. */
internal class AndroidUpdateNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val trackedAppsRepository: TrackedAppsRepository,
    ) : UpdateNotifier {
        private val notificationManager = NotificationManagerCompat.from(context)

        @SuppressLint("MissingPermission") // Checked by canNotify().
        override suspend fun notifyUpdates(apps: List<TrackedApp>) {
            if (!canNotify()) return
            createChannel()
            val (withUpdates, withoutUpdates) = apps.partition { it.updateStatus == UpdateStatus.UpdateAvailable }
            withoutUpdates.forEach { notificationManager.cancel(notificationId(it.id)) }
            withUpdates
                .filter { app -> app.latestRelease?.id != app.notifiedReleaseId }
                .forEach { app ->
                    val release = app.latestRelease ?: return@forEach
                    notificationManager.notify(notificationId(app.id), updateNotification(app))
                    trackedAppsRepository.setNotifiedRelease(app.id, release.id)
                }
            if (withUpdates.isEmpty()) {
                notificationManager.cancel(SUMMARY_ID)
            } else {
                notificationManager.notify(SUMMARY_ID, summaryNotification(withUpdates.size))
            }
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
                    .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.core_work_channel_updates))
                    .setDescription(context.getString(R.string.core_work_channel_updates_description))
                    .build(),
            )
        }

        private fun updateNotification(app: TrackedApp) =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.core_work_ic_notification)
                .setContentTitle(context.getString(R.string.core_work_update_title, app.displayName))
                .setContentText(versionText(app))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .build()

        private fun versionText(app: TrackedApp): String {
            val tag = app.latestRelease?.tagName.orEmpty()
            val installed = app.installedApp?.versionName
            return if (installed != null) {
                context.getString(R.string.core_work_update_text, installed, tag)
            } else {
                context.getString(R.string.core_work_update_text_unknown_version, tag)
            }
        }

        private fun summaryNotification(count: Int) =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.core_work_ic_notification)
                .setContentTitle(context.resources.getQuantityString(R.plurals.core_work_updates_summary, count, count))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .build()

        private fun openAppIntent(): PendingIntent? {
            val intent =
                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    ?: return null
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        private fun notificationId(appId: Long): Int = (appId % Int.MAX_VALUE).toInt() + APP_ID_OFFSET

        private companion object {
            const val CHANNEL_ID = "updates"
            const val GROUP_KEY = "me.geckotv.apkmanager.UPDATES"
            const val SUMMARY_ID = 1
            const val APP_ID_OFFSET = 1_000
        }
    }
