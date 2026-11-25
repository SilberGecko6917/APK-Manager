package me.geckotv.apkmanager.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    fun providesClock(): Clock = Clock.systemUTC()
}
