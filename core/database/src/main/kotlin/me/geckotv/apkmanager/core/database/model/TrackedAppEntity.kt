package me.geckotv.apkmanager.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** An app whose releases are tracked. Timestamps are epoch milliseconds. */
@Entity(
    tableName = "tracked_apps",
    indices = [
        Index(value = ["owner", "name"], unique = true),
        Index(value = ["package_name"]),
    ],
)
data class TrackedAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Repository owner; compared case-insensitively like on GitHub. */
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val owner: String,
    /** Repository name; compared case-insensitively like on GitHub. */
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val description: String?,
    @ColumnInfo(name = "owner_avatar_url")
    val ownerAvatarUrl: String?,
    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean,
    @ColumnInfo(name = "package_name")
    val packageName: String?,
    @ColumnInfo(name = "include_prereleases")
    val includePrereleases: Boolean,
    @ColumnInfo(name = "preferred_asset_name")
    val preferredAssetName: String?,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
    @ColumnInfo(name = "last_checked_at")
    val lastCheckedAt: Long?,
    /** ETag of the last release response, sent as `If-None-Match` by unauthenticated checks. */
    @ColumnInfo(name = "release_etag")
    val releaseEtag: String?,
)
