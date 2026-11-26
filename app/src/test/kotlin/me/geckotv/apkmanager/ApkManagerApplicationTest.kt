package me.geckotv.apkmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Starts the real [ApkManagerApplication] with its Hilt graph, the way Android does. A dependency that
 * touches WorkManager while Hilt injects the application would crash here (alpha.7 did).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ApkManagerApplication::class)
class ApkManagerApplicationTest {
    @Test
    fun applicationStartsAndInitializesWorkManager() {
        val application = ApplicationProvider.getApplicationContext<Context>() as ApkManagerApplication

        assertNotNull(application.workManagerConfiguration.workerFactory)
        assertNotNull(WorkManager.getInstance(application))
    }
}
