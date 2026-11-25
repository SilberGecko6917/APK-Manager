package me.geckotv.apkmanager.core.testing

import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.RepositoryPreview
import me.geckotv.apkmanager.core.model.TrackedApp
import java.time.Instant

/** Sample data for tests and screenshot tests. */
object TestData {
    val publishedAt: Instant = Instant.parse("2026-11-20T12:00:00Z")

    fun asset(
        id: Long,
        name: String,
        size: Long = 12_345_678,
    ) = ReleaseAsset(
        id = id,
        name = name,
        contentType = "application/vnd.android.package-archive",
        size = size,
        sha256 = null,
        downloadCount = 1_234,
        browserDownloadUrl = "https://github.com/octo/app/releases/download/v1/$name",
    )

    fun release(
        id: Long,
        tag: String,
        isPrerelease: Boolean = false,
        assets: List<ReleaseAsset> = listOf(asset(id * 10, "app-$tag.apk")),
    ) = Release(
        id = id,
        tagName = tag,
        name = "Release $tag",
        body = "## Changes\n\n- Something new in $tag",
        isPrerelease = isPrerelease,
        publishedAt = publishedAt,
        htmlUrl = "https://github.com/octo/app/releases/tag/$tag",
        assets = assets,
    )

    fun installedApp(
        packageName: String,
        label: String,
        versionName: String?,
        versionCode: Long = 1,
        isSystemApp: Boolean = false,
    ) = InstalledApp(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        isSystemApp = isSystemApp,
    )

    fun trackedApp(
        id: Long,
        repository: GitHubRepository,
        latestRelease: Release? = release(id, "v1.0.0"),
        installedApp: InstalledApp? = null,
        packageName: String? = installedApp?.packageName,
        includePrereleases: Boolean = false,
        isPrivate: Boolean = false,
    ) = TrackedApp(
        id = id,
        repository = repository,
        displayName = installedApp?.label ?: repository.name,
        description = "Description of ${repository.fullName}",
        ownerAvatarUrl = null,
        isPrivate = isPrivate,
        packageName = packageName,
        includePrereleases = includePrereleases,
        preferredAssetName = null,
        latestRelease = latestRelease,
        installedApp = installedApp,
        addedAt = publishedAt,
        lastCheckedAt = publishedAt,
    )

    fun preview(
        repository: GitHubRepository,
        releases: List<Release> = listOf(release(1, "v1.0.0")),
        isPrivate: Boolean = false,
    ) = RepositoryPreview(
        repository = repository,
        description = "Description of ${repository.fullName}",
        ownerAvatarUrl = null,
        isPrivate = isPrivate,
        isArchived = false,
        stars = 1_234,
        releases = releases,
    )
}
