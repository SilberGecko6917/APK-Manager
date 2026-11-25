package me.geckotv.apkmanager.core.network.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import me.geckotv.apkmanager.core.network.GitHubNetworkDataSource
import me.geckotv.apkmanager.core.network.RateLimitTracker
import me.geckotv.apkmanager.core.network.internal.GitHubApi
import me.geckotv.apkmanager.core.network.internal.RetrofitGitHubNetwork
import me.geckotv.apkmanager.core.network.internal.createGitHubApi
import me.geckotv.apkmanager.core.network.internal.createGitHubOkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private const val GITHUB_API_URL = "https://api.github.com/"

    @Provides
    @Singleton
    fun providesJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        credentialsProvider: GitHubCredentialsProvider,
        rateLimitTracker: RateLimitTracker,
    ): OkHttpClient =
        createGitHubOkHttpClient(
            apiHost = GITHUB_API_URL.toHttpUrl().host,
            credentialsProvider = credentialsProvider,
            rateLimitTracker = rateLimitTracker,
        )

    @Provides
    @Singleton
    fun providesGitHubApi(
        okHttpClient: OkHttpClient,
        json: Json,
    ): GitHubApi = createGitHubApi(GITHUB_API_URL, okHttpClient, json)

    /** Unauthenticated until GitHub sign-in is implemented. */
    @Provides
    @Singleton
    fun providesCredentialsProvider(): GitHubCredentialsProvider = GitHubCredentialsProvider { null }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkBindingsModule {
    @Binds
    fun bindsGitHubNetworkDataSource(network: RetrofitGitHubNetwork): GitHubNetworkDataSource
}
