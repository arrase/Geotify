package dev.arrase.geotify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity

@Database(
    entities = [LocationEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = true
)
abstract class GeotifyDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: GeotifyDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE locations ADD COLUMN notification_responsiveness_ms INTEGER NOT NULL DEFAULT 120000")
            }
        }

        fun getInstance(context: Context): GeotifyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeotifyDatabase::class.java,
                    "geotify.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
    }
}
