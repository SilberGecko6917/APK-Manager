package me.geckotv.apkmanager.core.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import me.geckotv.apkmanager.core.database.dao.TrackedAppDao
import me.geckotv.apkmanager.core.database.model.LatestReleaseEntity
import me.geckotv.apkmanager.core.database.model.ReleaseAssetEntity
import me.geckotv.apkmanager.core.database.model.TrackedAppEntity

/**
 * The app's database. Schema changes must bump [version] and add a migration (prefer
 * `autoMigrations`); exported schemas live in `core/database/schemas`.
 */
@Database(
    entities = [
        TrackedAppEntity::class,
        LatestReleaseEntity::class,
        ReleaseAssetEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // 2: tracked_apps.notified_release_id
        AutoMigration(from = 1, to = 2),
        // 3: asset filter, version pattern, installed and skipped release
        AutoMigration(from = 2, to = 3),
        // 4: update policy, release of the last automatic update
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class ApkManagerDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao
}
