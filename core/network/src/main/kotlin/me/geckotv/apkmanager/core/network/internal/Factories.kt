package me.geckotv.apkmanager.core.network.internal

import kotlinx.serialization.json.Json
import me.geckotv.apkmanager.core.network.GitHubCredentialsProvider
import me.geckotv.apkmanager.core.network.RateLimitTracker
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Duration

private val CONNECT_TIMEOUT = Duration.ofSeconds(15)
private val READ_TIMEOUT = Duration.ofSeconds(30)

internal fun createGitHubOkHttpClient(
    apiHost: String,
    credentialsProvider: GitHubCredentialsProvider,
    rateLimitTracker: RateLimitTracker,
): OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(CONNECT_TIMEOUT)
        .readTimeout(READ_TIMEOUT)
        .addInterceptor(GitHubRequestInterceptor(apiHost, credentialsProvider))
        .addInterceptor(RateLimitInterceptor(rateLimitTracker))
        .build()

internal fun createGitHubApi(
    baseUrl: String,
    okHttpClient: OkHttpClient,
    json: Json,
): GitHubApi =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GitHubApi::class.java)
