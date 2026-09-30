package dev.arrase.geotify.data

import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun observeLocations(): Flow<List<LocationEntity>> = locationDao.observeAll()

    suspend fun saveLocation(
        alias: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 150f,
        notificationResponsivenessMs: Int = 0
    ): LocationEntity = withContext(ioDispatcher) {
        val normalizedAlias = alias.trim()
        require(normalizedAlias.isNotEmpty()) { "Alias must not be blank" }
        validate(latitude, longitude, radiusMeters, notificationResponsivenessMs)
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            alias = normalizedAlias,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            notificationResponsivenessMs = notificationResponsivenessMs
        )
        if (locationDao.insert(entity) == -1L) {
            throw DuplicateAliasException(normalizedAlias)
        }
        entity
    }

    suspend fun updateLocation(location: LocationEntity) = withContext(ioDispatcher) {
        require(location.alias.isNotBlank()) { "Alias must not be blank" }
        validate(
            location.latitude,
            location.longitude,
            location.radiusMeters,
            location.notificationResponsivenessMs
        )
        locationDao.update(location.copy(alias = location.alias.trim()))
    }

    suspend fun getAllLocations(): List<LocationEntity> = withContext(ioDispatcher) {
        locationDao.getAll()
    }

    suspend fun findLocationByAlias(alias: String): LocationEntity? = withContext(ioDispatcher) {
        locationDao.findByAlias(alias)
    }

    suspend fun findLocationById(id: String): LocationEntity? = withContext(ioDispatcher) {
        locationDao.findById(id)
    }

    /**
     * Bounding-box prefilter. May return candidates outside [minLat]..[maxLat] range when the
     * longitude window straddles the antimeridian; callers must still check exact distance.
     */
    suspend fun findLocationsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<LocationEntity> = withContext(ioDispatcher) {
        locationDao.getLocationsInBoundingBox(minLat, maxLat, minLon, maxLon)
    }

    suspend fun getAllAliases(): List<String> = withContext(ioDispatcher) {
        locationDao.getAllAliases()
    }

    /** @return `true` if a location was removed. */
    suspend fun deleteLocation(alias: String): Boolean = withContext(ioDispatcher) {
        locationDao.deleteByAlias(alias) > 0
    }

    private fun validate(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        notificationResponsivenessMs: Int
    ) {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        require(radiusMeters >= MIN_RADIUS_METERS) {
            "Geofence radius must be at least $MIN_RADIUS_METERS meters"
        }
        require(notificationResponsivenessMs >= 0) { "Notification responsiveness must be non-negative" }
    }

    private companion object {
        const val MIN_RADIUS_METERS = 50f
    }
}

/** Thrown when saving a location whose alias is already taken (case-insensitive). */
class DuplicateAliasException(alias: String) :
    IllegalArgumentException("Alias '$alias' already exists")

