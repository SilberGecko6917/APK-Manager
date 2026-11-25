package me.geckotv.apkmanager.core.model

/** The GitHub account the user signed in with. */
data class GitHubAccount(
    val login: String,
    val name: String?,
    val avatarUrl: String?,
    val authMethod: AuthMethod,
    /** What the token can access; `null` when it cannot be determined (fine-grained tokens). */
    val accessLevel: GitHubAccessLevel?,
)

/** How the user signed in. */
enum class AuthMethod {
    /** OAuth device flow ("Sign in with GitHub"). */
    OAuth,

    /** A personal access token pasted by the user. */
    PersonalAccessToken,
}

/** Which repositories the app may read with the user's token. */
enum class GitHubAccessLevel {
    /** Public repositories only (OAuth: no scope). */
    Public,

    /** Public and private repositories (OAuth: `repo` scope). */
    Private,
}
