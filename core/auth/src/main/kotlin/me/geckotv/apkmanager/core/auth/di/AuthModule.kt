package me.geckotv.apkmanager.core.auth.di

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
import kotlinx.serialization.json.Json
import me.geckotv.apkmanager.core.auth.AuthRepository
import me.geckotv.apkmanager.core.auth.internal.CachedCredentialsProvider
import me.geckotv.apkmanager.core.auth.internal.DefaultAuthRepository
import me.geckotv.apkmanager.core.auth.internal.GitHubOAuthApi
import me.geckotv.apkmanager.core.auth.internal.KeystoreTokenCipher
import me.geckotv.apkmanager.core.auth.internal.TokenCipher
import me.geckotv.apkmanager.core.common.ApkManagerDispatchers
import me.geckotv.apkmanager.core.common.Dispatcher
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

/** The DataStore that holds the encrypted token. Its file is excluded from backups. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CredentialsDataStore

@Module
@InstallIn(SingletonComponent::class)
internal object AuthModule {
    /** Must match the backup exclusion rules in the app module. */
    const val CREDENTIALS_FILE = "credentials"
    private const val GITHUB_URL = "https://github.com/"

    @Provides
    @Singleton
    @CredentialsDataStore
    fun providesCredentialsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(ApkManagerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + ioDispatcher),
            produceFile = { context.preferencesDataStoreFile(CREDENTIALS_FILE) },
        )

    @Provides
    @Singleton
    fun providesGitHubOAuthApi(
        okHttpClient: OkHttpClient,
        json: Json,
    ): GitHubOAuthApi =
        Retrofit
            .Builder()
            .baseUrl(GITHUB_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubOAuthApi::class.java)

    @Provides
    fun providesClock(): Clock = Clock.systemUTC()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthBindingsModule {
    @Binds
    fun bindsAuthRepository(repository: DefaultAuthRepository): AuthRepository

    @Binds
    fun bindsCredentialsProvider(provider: CachedCredentialsProvider): GitHubCredentialsProvider

    @Binds
    fun bindsTokenCipher(cipher: KeystoreTokenCipher): TokenCipher
}
