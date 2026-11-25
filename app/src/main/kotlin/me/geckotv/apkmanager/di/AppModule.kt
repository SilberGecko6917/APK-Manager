package me.geckotv.apkmanager.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.geckotv.apkmanager.BuildConfig
import me.geckotv.apkmanager.core.auth.GitHubOAuthConfig

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun providesGitHubOAuthConfig(): GitHubOAuthConfig = GitHubOAuthConfig(clientId = BuildConfig.GITHUB_CLIENT_ID)
}
