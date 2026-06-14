package dev.arrase.geotify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity

@Database(
    entities = [LocationEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GeotifyDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: GeotifyDatabase? = null

        fun getInstance(context: Context): GeotifyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeotifyDatabase::class.java,
                    "geotify.db"
                ).build().also { INSTANCE = it }
            }
    }
}
