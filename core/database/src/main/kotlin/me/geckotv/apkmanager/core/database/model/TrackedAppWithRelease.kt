package me.geckotv.apkmanager.core.database.model

import androidx.room3.Embedded
import androidx.room3.Relation

/** A tracked app with its latest release and the release's assets. */
data class TrackedAppWithRelease(
    @Embedded
    val app: TrackedAppEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["app_id"])
    val latestRelease: LatestReleaseEntity?,
    @Relation(parentColumns = ["id"], entityColumns = ["app_id"])
    val assets: List<ReleaseAssetEntity>,
)
