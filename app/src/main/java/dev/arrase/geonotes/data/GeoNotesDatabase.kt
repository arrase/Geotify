package dev.arrase.geonotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.arrase.geonotes.data.dao.LocationDao
import dev.arrase.geonotes.data.dao.ReminderDao
import dev.arrase.geonotes.data.entity.LocationEntity
import dev.arrase.geonotes.data.entity.ReminderEntity

@Database(
    entities = [LocationEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GeoNotesDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: GeoNotesDatabase? = null

        fun getInstance(context: Context): GeoNotesDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GeoNotesDatabase::class.java,
                    "geonotes.db"
                ).build().also { INSTANCE = it }
            }
    }
}
