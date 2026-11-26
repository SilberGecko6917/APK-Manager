package me.geckotv.apkmanager.core.work.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.geckotv.apkmanager.core.work.UpdateNotifier
import me.geckotv.apkmanager.core.work.internal.AndroidUpdateNotifier

@Module
@InstallIn(SingletonComponent::class)
internal object WorkProvidersModule {
    @Provides
    fun providesWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface WorkModule {
    @Binds
    fun bindsUpdateNotifier(notifier: AndroidUpdateNotifier): UpdateNotifier
}
