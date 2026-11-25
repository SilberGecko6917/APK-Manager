package me.geckotv.apkmanager.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey

/** The newest release that counts for a tracked app (one row per app). */
@Entity(
    tableName = "latest_releases",
    foreignKeys = [
        ForeignKey(
            entity = TrackedAppEntity::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LatestReleaseEntity(
    @PrimaryKey
    @ColumnInfo(name = "app_id")
    val appId: Long,
    /** GitHub's release ID. */
    @ColumnInfo(name = "release_id")
    val releaseId: Long,
    @ColumnInfo(name = "tag_name")
    val tagName: String,
    val name: String?,
    val body: String?,
    @ColumnInfo(name = "is_prerelease")
    val isPrerelease: Boolean,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "published_at")
    val publishedAt: Long?,
    @ColumnInfo(name = "html_url")
    val htmlUrl: String,
)
