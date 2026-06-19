package dev.arrase.geotify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT location_id, COUNT(*) as count FROM reminders WHERE is_active = 1 GROUP BY location_id")
    fun observeActiveReminderCounts(): Flow<List<LocationReminderCount>>

    @Query("SELECT COUNT(DISTINCT location_id) FROM reminders WHERE is_active = 1")
    suspend fun getActiveGeofenceCount(): Int



    @Query(
        """
        SELECT r.* FROM reminders r
        INNER JOIN locations l ON r.location_id = l.id
        ORDER BY r.is_active DESC, r.created_at DESC
        """
    )
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE is_active = 1")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun findById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE location_id = :locationId AND is_active = 1")
    suspend fun getActiveByLocationId(locationId: String): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders WHERE location_id = :locationId AND is_active = 1")
    fun observeActiveCountForLocation(locationId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        SELECT r.id FROM reminders r
        INNER JOIN locations l ON r.location_id = l.id
        WHERE l.alias = :alias COLLATE NOCASE AND r.is_active = 1
        """
    )
    suspend fun getActiveReminderIdsByAlias(alias: String): List<String>

    @Query("UPDATE reminders SET is_in_range = 0")
    suspend fun clearAllInRange()

    @Query("UPDATE reminders SET is_in_range = 1 WHERE location_id IN (:locationIds) AND is_active = 1")
    suspend fun setInRangeForLocations(locationIds: List<String>)

    @Transaction
    suspend fun updateInRangeStatus(locationIds: List<String>) {
        clearAllInRange()
        if (locationIds.isNotEmpty()) {
            setInRangeForLocations(locationIds)
        }
    }
}
