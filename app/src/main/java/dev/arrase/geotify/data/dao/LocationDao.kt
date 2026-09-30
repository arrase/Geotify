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

    /** @return the new row id, or `-1` if the alias already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(location: LocationEntity): Long

    @Update
    suspend fun update(location: LocationEntity)

    @Query("DELETE FROM locations WHERE alias = :alias COLLATE NOCASE")
    suspend fun deleteByAlias(alias: String): Int

    /**
     * Bounding-box prefilter for [dev.arrase.geotify.domain.SpatialSearchUseCase].
     *
     * The longitude window is tested three times — as given, shifted by -360 and by +360 — so that a
     * window straddling the antimeridian (e.g. 174..184) still matches points stored on the western
     * side (e.g. -179). Results may be a superset of the true radius; callers must still apply an exact
     * distance check.
     */
    @Query("""
        SELECT * FROM locations
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND (
                (longitude BETWEEN :minLon AND :maxLon)
             OR (longitude BETWEEN :minLon - 360.0 AND :maxLon - 360.0)
             OR (longitude BETWEEN :minLon + 360.0 AND :maxLon + 360.0)
          )
    """)
    suspend fun getLocationsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<LocationEntity>
}
