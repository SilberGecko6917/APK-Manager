package me.geckotv.apkmanager.core.data.internal

import me.geckotv.apkmanager.core.database.model.LatestReleaseEntity
import me.geckotv.apkmanager.core.database.model.ReleaseAssetEntity
import me.geckotv.apkmanager.core.database.model.TrackedAppWithRelease
import me.geckotv.apkmanager.core.model.AssetFilter
import me.geckotv.apkmanager.core.model.GitHubRepository
import me.geckotv.apkmanager.core.model.InstalledApp
import me.geckotv.apkmanager.core.model.InstalledRelease
import me.geckotv.apkmanager.core.model.Release
import me.geckotv.apkmanager.core.model.ReleaseAsset
import me.geckotv.apkmanager.core.model.TrackedApp
import me.geckotv.apkmanager.core.model.UpdatePolicy
import java.time.Instant

internal fun Release.toEntity(appId: Long): LatestReleaseEntity =
    LatestReleaseEntity(
        appId = appId,
        releaseId = id,
        tagName = tagName,
        name = name,
        body = body,
        isPrerelease = isPrerelease,
        publishedAt = publishedAt?.toEpochMilli(),
        htmlUrl = htmlUrl,
    )

internal fun Release.assetEntities(appId: Long): List<ReleaseAssetEntity> =
    assets.map { asset ->
        ReleaseAssetEntity(
            id = asset.id,
            appId = appId,
            name = asset.name,
            contentType = asset.contentType,
            size = asset.size,
            sha256 = asset.sha256,
            downloadCount = asset.downloadCount,
            browserDownloadUrl = asset.browserDownloadUrl,
        )
    }

internal val TrackedAppWithRelease.repository: GitHubRepository
    get() = GitHubRepository(owner = app.owner, name = app.name)

internal fun TrackedAppWithRelease.toTrackedApp(installedApp: InstalledApp?): TrackedApp =
    TrackedApp(
        id = app.id,
        repository = repository,
        displayName = installedApp?.label ?: app.displayName,
        description = app.description,
        ownerAvatarUrl = app.ownerAvatarUrl,
        isPrivate = app.isPrivate,
        packageName = app.packageName,
        includePrereleases = app.includePrereleases,
        preferredAssetName = app.preferredAssetName,
        latestRelease =
            latestRelease?.let { release ->
                Release(
                    id = release.releaseId,
                    tagName = release.tagName,
                    name = release.name,
                    body = release.body,
                    isPrerelease = release.isPrerelease,
                    publishedAt = release.publishedAt?.let(Instant::ofEpochMilli),
                    htmlUrl = release.htmlUrl,
                    assets =
                        assets.sortedBy { it.id }.map { asset ->
                            ReleaseAsset(
                                id = asset.id,
                                name = asset.name,
                                contentType = asset.contentType,
                                size = asset.size,
                                sha256 = asset.sha256,
                                downloadCount = asset.downloadCount,
                                browserDownloadUrl = asset.browserDownloadUrl,
                            )
                        },
                )
            },
        installedApp = installedApp,
        addedAt = Instant.ofEpochMilli(app.addedAt),
        lastCheckedAt = app.lastCheckedAt?.let(Instant::ofEpochMilli),
        notifiedReleaseId = app.notifiedReleaseId,
        assetFilter = AssetFilter(include = app.assetIncludePattern, exclude = app.assetExcludePattern),
        versionPattern = app.versionPattern,
        installedRelease =
            app.installedReleaseId?.let { releaseId ->
                app.installedVersionCode?.let { versionCode -> InstalledRelease(releaseId, versionCode) }
            },
        skippedReleaseId = app.skippedReleaseId,
        updatePolicy = app.updatePolicy?.let { name -> UpdatePolicy.entries.firstOrNull { it.name == name } },
        autoUpdateReleaseId = app.autoUpdateReleaseId,
    )
