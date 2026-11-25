package me.geckotv.apkmanager.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A file attached to the latest release of a tracked app. */
@Entity(
    tableName = "release_assets",
    foreignKeys = [
        ForeignKey(
            entity = LatestReleaseEntity::class,
            parentColumns = ["app_id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["app_id"])],
)
data class ReleaseAssetEntity(
    /** GitHub's asset ID. */
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "app_id")
    val appId: Long,
    val name: String,
    @ColumnInfo(name = "content_type")
    val contentType: String?,
    val size: Long,
    /** Lowercase hex SHA-256 digest, if GitHub provides one. */
    val sha256: String?,
    @ColumnInfo(name = "download_count")
    val downloadCount: Long,
    @ColumnInfo(name = "browser_download_url")
    val browserDownloadUrl: String,
)
