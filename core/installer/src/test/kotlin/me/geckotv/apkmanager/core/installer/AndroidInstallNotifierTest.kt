package me.geckotv.apkmanager.core.installer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import me.geckotv.apkmanager.core.installer.internal.AndroidInstallNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AndroidInstallNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val notifier = AndroidInstallNotifier(context)

    @Test
    fun download_showsProgressAndCancel() {
        notifier.show(appId = 1, name = "Notes", state = InstallState.Downloading(bytesRead = 400, totalBytes = 1000))

        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals("Downloading Notes", shadowOf(notification).contentTitle)
        assertEquals(40, shadowOf(notification).progress)
        assertEquals(100, shadowOf(notification).max)
        assertEquals("Cancel", notification.actions.single().title)
    }

    @Test
    fun waitingForUser_asksToTap() {
        notifier.show(appId = 1, name = "Notes", state = InstallState.WaitingForUser(Intent("confirm")))

        val notification = shadowOf(notificationManager).allNotifications.single()
        assertEquals("Tap to install Notes", shadowOf(notification).contentTitle)
        assertTrue(notification.actions.isNullOrEmpty())
    }

    @Test
    fun cancel_removesTheNotification() {
        notifier.show(appId = 1, name = "Notes", state = InstallState.Installing)

        notifier.cancel(1)

        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    @Test
    fun summary_countsRunningInstalls() {
        val summary = notifier.summary(count = 2)

        assertEquals("Updating 2 apps", shadowOf(summary).contentTitle)
    }
}
