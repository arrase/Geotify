package dev.arrase.geotify.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.arrase.geotify.data.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY alias ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY alias ASC")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE alias = :alias COLLATE NOCASE LIMIT 1")
    suspend fun findByAlias(alias: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun findById(id: String): LocationEntity?

    @Query("SELECT alias FROM locations ORDER BY alias ASC")
    suspend fun getAllAliases(): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(location: LocationEntity)

    @Update
    suspend fun update(location: LocationEntity)

    @Query("DELETE FROM locations WHERE alias = :alias COLLATE NOCASE")
    suspend fun deleteByAlias(alias: String): Int

    @Query("SELECT * FROM locations WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    suspend fun getLocationsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<LocationEntity>
}
