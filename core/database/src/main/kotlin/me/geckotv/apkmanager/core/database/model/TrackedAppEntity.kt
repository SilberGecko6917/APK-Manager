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
    /** GitHub ID of the release the user was last notified about, so each update is notified once. */
    @ColumnInfo(name = "notified_release_id")
    val notifiedReleaseId: Long? = null,
    /** Regular expression an asset name must contain, `null` for any. */
    @ColumnInfo(name = "asset_include_pattern")
    val assetIncludePattern: String? = null,
    /** Regular expression an asset name must not contain, `null` for none. */
    @ColumnInfo(name = "asset_exclude_pattern")
    val assetExcludePattern: String? = null,
    /** Regular expression that extracts the version from a release tag. */
    @ColumnInfo(name = "version_pattern")
    val versionPattern: String? = null,
    /** GitHub ID of the release APK Manager installed, with the version code of its APK. */
    @ColumnInfo(name = "installed_release_id")
    val installedReleaseId: Long? = null,
    @ColumnInfo(name = "installed_version_code")
    val installedVersionCode: Long? = null,
    /** GitHub ID of a release the user chose to skip. */
    @ColumnInfo(name = "skipped_release_id")
    val skippedReleaseId: Long? = null,
    /** Name of the app's update policy, `null` to follow the default in the settings. */
    @ColumnInfo(name = "update_policy")
    val updatePolicy: String? = null,
    /** GitHub ID of the release APK Manager last tried to install automatically. */
    @ColumnInfo(name = "auto_update_release_id")
    val autoUpdateReleaseId: Long? = null,
)
