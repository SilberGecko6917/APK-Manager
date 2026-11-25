package me.geckotv.apkmanager.core.database

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
    version = 1,
    exportSchema = true,
)
abstract class ApkManagerDatabase : RoomDatabase() {
    abstract fun trackedAppDao(): TrackedAppDao
}
