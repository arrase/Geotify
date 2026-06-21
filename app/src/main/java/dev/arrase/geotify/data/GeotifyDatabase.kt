package dev.arrase.geotify.data

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity

@Database(
    entities = [LocationEntity::class, ReminderEntity::class],
    version = 3,
    exportSchema = true
)
abstract class GeotifyDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun reminderDao(): ReminderDao
}
