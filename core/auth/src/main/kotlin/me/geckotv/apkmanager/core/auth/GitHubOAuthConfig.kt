package me.geckotv.apkmanager.core.auth

/**
 * Configuration of the GitHub OAuth app used for "Sign in with GitHub".
 *
 * @property clientId the public client ID of the OAuth app. Device flow must be enabled for the app.
 */
data class GitHubOAuthConfig(
    val clientId: String,
)
