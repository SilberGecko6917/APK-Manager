package me.geckotv.apkmanager.core.model

/** A repository looked up before it is added, with its newest releases. */
data class RepositoryPreview(
    val repository: GitHubRepository,
    val description: String?,
    val ownerAvatarUrl: String?,
    val isPrivate: Boolean,
    val isArchived: Boolean,
    val stars: Int,
    /** Published releases, newest first (pre-releases included). */
    val releases: List<Release>,
) {
    /** The release that counts when pre-releases are included or not. */
    fun latestRelease(includePrereleases: Boolean): Release? =
        releases.firstOrNull { includePrereleases || !it.isPrerelease }
}
