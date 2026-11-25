package me.geckotv.apkmanager.core.model

import java.time.Instant

/** A published (non-draft) GitHub release. */
data class Release(
    /** GitHub's release ID. */
    val id: Long,
    val tagName: String,
    /** The release title, if it has one. */
    val name: String?,
    /** Release notes in GitHub Flavored Markdown. */
    val body: String?,
    val isPrerelease: Boolean,
    val publishedAt: Instant?,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>,
)

/** A file attached to a release. */
data class ReleaseAsset(
    /** GitHub's asset ID. */
    val id: Long,
    val name: String,
    val contentType: String?,
    /** Size in bytes. */
    val size: Long,
    /** Lowercase hex SHA-256 digest, if GitHub provides one (newer assets only). */
    val sha256: String?,
    val downloadCount: Long,
    /** Public download URL; does not work for private repositories. */
    val browserDownloadUrl: String,
)
