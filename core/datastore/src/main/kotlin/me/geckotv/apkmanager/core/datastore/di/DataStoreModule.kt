package me.geckotv.apkmanager.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.datastore.DataStoreUserSettingsRepository
import me.geckotv.apkmanager.core.datastore.UserSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    /** File name of the settings DataStore. It holds no secrets, so it may be backed up. */
    private const val USER_SETTINGS_FILE = "user_settings"

    @Provides
    @Singleton
    fun providesUserSettingsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(ApkManagerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(USER_SETTINGS_FILE) },
        )
}

@Module
@InstallIn(SingletonComponent::class)
internal interface UserSettingsModule {
    @Binds
    fun bindsUserSettingsRepository(repository: DataStoreUserSettingsRepository): UserSettingsRepository
}
