package me.geckotv.apkmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import me.geckotv.apkmanager.core.installer.InstallNotifications
import me.geckotv.apkmanager.core.work.UpdateCheckScheduler
import javax.inject.Inject

@HiltAndroidApp
class ApkManagerApplication :
    Application(),
    SingletonImageLoader.Factory,
    Configuration.Provider {
    @Inject
    lateinit var selfUpdateTracking: SelfUpdateTracking

    @Inject
    lateinit var updateCheckScheduler: UpdateCheckScheduler

    @Inject
    lateinit var installNotifications: InstallNotifications

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** WorkManager is initialized on demand with Hilt's worker factory (see the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        updateCheckScheduler.start()
        installNotifications.start()
        // Debug builds have their own package name and signing key, so they cannot update from releases.
        if (!BuildConfig.DEBUG) {
            selfUpdateTracking.start(
                repository = BuildConfig.SELF_REPOSITORY,
                packageName = BuildConfig.APPLICATION_ID,
                versionName = BuildConfig.VERSION_NAME,
            )
        }
    }

    /**
     * The image loader for all `AsyncImage`s, e.g. repository owner avatars. It uses its own
     * HTTP client, so avatar requests never carry the GitHub token.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .build()
}
