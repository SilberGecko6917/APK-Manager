package me.geckotv.apkmanager

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApkManagerApplication :
    Application(),
    SingletonImageLoader.Factory {
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
