package me.geckotv.apkmanager.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import me.geckotv.apkmanager.core.database.model.LatestReleaseEntity
import me.geckotv.apkmanager.core.database.model.ReleaseAssetEntity
import me.geckotv.apkmanager.core.database.model.TrackedAppEntity
import me.geckotv.apkmanager.core.database.model.TrackedAppWithRelease

// A DAO has one function per query.
@Suppress("TooManyFunctions")
@Dao
interface TrackedAppDao {
    @Transaction
    @Query("SELECT * FROM tracked_apps ORDER BY display_name COLLATE NOCASE")
    fun observeTrackedApps(): Flow<List<TrackedAppWithRelease>>

    @Transaction
    @Query("SELECT * FROM tracked_apps WHERE id = :id")
    fun observeTrackedApp(id: Long): Flow<TrackedAppWithRelease?>

    @Transaction
    @Query("SELECT * FROM tracked_apps")
    suspend fun getTrackedApps(): List<TrackedAppWithRelease>

    @Query("SELECT * FROM tracked_apps WHERE id = :id")
    suspend fun getTrackedApp(id: Long): TrackedAppEntity?

    /** Finds an app by repository, ignoring case. */
    @Query("SELECT * FROM tracked_apps WHERE owner = :owner AND name = :name")
    suspend fun findByRepository(
        owner: String,
        name: String,
    ): TrackedAppEntity?

    /** Inserts [app] and returns its ID. Fails if the repository is already tracked. */
    @Insert
    suspend fun insertTrackedApp(app: TrackedAppEntity): Long

    @Query("DELETE FROM tracked_apps WHERE id = :id")
    suspend fun deleteTrackedApp(id: Long)

    @Query("UPDATE tracked_apps SET package_name = :packageName WHERE id = :id")
    suspend fun setPackageName(
        id: Long,
        packageName: String?,
    )

    @Query("UPDATE tracked_apps SET include_prereleases = :includePrereleases, release_etag = NULL WHERE id = :id")
    suspend fun setIncludePrereleases(
        id: Long,
        includePrereleases: Boolean,
    )

    @Query("UPDATE tracked_apps SET preferred_asset_name = :assetName WHERE id = :id")
    suspend fun setPreferredAssetName(
        id: Long,
        assetName: String?,
    )

    @Query("UPDATE tracked_apps SET notified_release_id = :releaseId WHERE id = :id")
    suspend fun setNotifiedRelease(
        id: Long,
        releaseId: Long,
    )

    /** Records a successful release check at [checkedAt] (epoch milliseconds). */
    @Query("UPDATE tracked_apps SET last_checked_at = :checkedAt, release_etag = :etag WHERE id = :id")
    suspend fun setChecked(
        id: Long,
        checkedAt: Long,
        etag: String?,
    )

    @Upsert
    suspend fun upsertLatestRelease(release: LatestReleaseEntity)

    @Query("DELETE FROM latest_releases WHERE app_id = :appId")
    suspend fun deleteLatestRelease(appId: Long)

    @Query("DELETE FROM release_assets WHERE app_id = :appId")
    suspend fun deleteAssets(appId: Long)

    @Insert
    suspend fun insertAssets(assets: List<ReleaseAssetEntity>)

    /** Replaces the latest release of app [appId] and its assets; `null` removes them. */
    @Transaction
    suspend fun replaceLatestRelease(
        appId: Long,
        release: LatestReleaseEntity?,
        assets: List<ReleaseAssetEntity>,
    ) {
        deleteAssets(appId)
        if (release == null) {
            deleteLatestRelease(appId)
        } else {
            upsertLatestRelease(release)
            insertAssets(assets)
        }
    }
}
