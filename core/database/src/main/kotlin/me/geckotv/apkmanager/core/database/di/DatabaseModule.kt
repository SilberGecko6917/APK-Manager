package me.geckotv.apkmanager.core.database.di

import android.content.Context
import androidx.room3.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.geckotv.apkmanager.core.database.ApkManagerDatabase
import me.geckotv.apkmanager.core.database.dao.TrackedAppDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    private const val DATABASE_NAME = "apk-manager.db"

    @Provides
    @Singleton
    fun providesDatabase(
        @ApplicationContext context: Context,
    ): ApkManagerDatabase = Room.databaseBuilder<ApkManagerDatabase>(context, DATABASE_NAME).build()

    @Provides
    fun providesTrackedAppDao(database: ApkManagerDatabase): TrackedAppDao = database.trackedAppDao()
}
