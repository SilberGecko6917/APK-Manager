package me.geckotv.apkmanager.core.model

/** A repository on GitHub, identified by [owner] and [name] (e.g. `SilberGecko6917` / `APK-Manager`). */
data class GitHubRepository(
    val owner: String,
    val name: String,
) {
    /** `owner/name`. */
    val fullName: String get() = "$owner/$name"

    /** The repository page on github.com. */
    val htmlUrl: String get() = "https://github.com/$owner/$name"

    override fun toString(): String = fullName
}
