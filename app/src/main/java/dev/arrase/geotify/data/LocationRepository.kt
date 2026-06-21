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
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        require(radiusMeters >= 50f) { "Geofence radius must be at least 50 meters" }
        require(notificationResponsivenessMs >= 0) { "Notification responsiveness must be non-negative" }
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            alias = alias,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            notificationResponsivenessMs = notificationResponsivenessMs
        )
        locationDao.insert(entity)
        entity
    }

    suspend fun updateLocation(location: LocationEntity) = withContext(ioDispatcher) {
        require(location.latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(location.longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        require(location.radiusMeters >= 50f) { "Geofence radius must be at least 50 meters" }
        locationDao.update(location)
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

    suspend fun getAllAliases(): List<String> = withContext(ioDispatcher) {
        locationDao.getAllAliases()
    }

    suspend fun deleteLocation(alias: String) = withContext(ioDispatcher) {
        locationDao.deleteByAlias(alias)
    }
}
