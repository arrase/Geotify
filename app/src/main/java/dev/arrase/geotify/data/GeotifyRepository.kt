package dev.arrase.geotify.data

import android.util.Log
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.geofence.GeofenceManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GeotifyRepository(
    private val locationDao: LocationDao,
    private val reminderDao: ReminderDao,
    private val geofenceManager: GeofenceManager
) {

    // ── Location Observation ──

    fun observeLocations(): Flow<List<LocationEntity>> = locationDao.observeAll()

    fun observeReminders(): Flow<List<ReminderEntity>> = reminderDao.observeAll()

    fun observeActiveReminderCounts(): Flow<List<LocationReminderCount>> =
        reminderDao.observeActiveReminderCounts()

    fun observeActiveReminderCount(locationId: String): Flow<Int> =
        reminderDao.observeActiveCountForLocation(locationId)

    // ── Location Operations ──

    suspend fun saveLocation(
        alias: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 150f
    ): LocationEntity {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            alias = alias,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters
        )
        locationDao.insert(entity)
        return entity
    }

    suspend fun updateLocation(location: LocationEntity) {
        require(location.latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(location.longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        locationDao.update(location)
        syncGeofenceForLocation(location.id)
    }

    suspend fun getAllLocations(): List<LocationEntity> = locationDao.getAll()

    suspend fun findLocationByAlias(alias: String): LocationEntity? =
        locationDao.findByAlias(alias)

    suspend fun getAllAliases(): List<String> = locationDao.getAllAliases()

    suspend fun deleteLocation(alias: String) {
        val location = locationDao.findByAlias(alias)
        if (location != null) {
            runCatching { geofenceManager.removeGeofences(listOf(location.id)) }
                .onFailure { Log.w(TAG, "Failed to remove geofence for deleted location: $alias", it) }
            locationDao.deleteByAlias(alias)
        }
    }

    // ── Reminder Operations ──

    suspend fun createReminder(
        location: LocationEntity,
        message: String,
        transitionType: Int
    ): ReminderEntity {
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            locationId = location.id,
            message = message,
            transitionType = transitionType,
            createdAt = System.currentTimeMillis()
        )
        reminderDao.insert(reminder)
        syncGeofenceForLocation(location.id)
        return reminder
    }

    suspend fun updateReminder(reminder: ReminderEntity, oldLocationId: String) {
        reminderDao.update(reminder)
        syncGeofenceForLocation(reminder.locationId)
        if (oldLocationId != reminder.locationId) {
            syncGeofenceForLocation(oldLocationId)
        }
    }

    suspend fun deactivateReminder(reminderId: String) {
        val reminder = reminderDao.findById(reminderId) ?: return
        reminderDao.deactivate(reminderId)
        syncGeofenceForLocation(reminder.locationId)
    }

    suspend fun cancelReminder(reminderId: String) {
        val reminder = reminderDao.findById(reminderId) ?: return
        reminderDao.deleteById(reminderId)
        syncGeofenceForLocation(reminder.locationId)
    }

    suspend fun findReminderById(id: String): ReminderEntity? = reminderDao.findById(id)

    suspend fun findLocationById(id: String): LocationEntity? = locationDao.findById(id)

    suspend fun getActiveReminders(): List<ReminderEntity> = reminderDao.getActiveReminders()

    suspend fun getActiveRemindersForLocation(locationId: String): List<ReminderEntity> =
        reminderDao.getActiveByLocationId(locationId)

    // ── Geofence Re-registration (after boot) ──

    suspend fun reRegisterAllActiveGeofences() {
        val locations = locationDao.getAll()
        for (location in locations) {
            syncGeofenceForLocation(location.id)
        }
    }

    // ── Sync Helper ──

    private suspend fun syncGeofenceForLocation(locationId: String) {
        val location = locationDao.findById(locationId)
        if (location == null) {
            runCatching { geofenceManager.removeGeofences(listOf(locationId)) }
                .onFailure { Log.w(TAG, "Failed to remove geofence for missing location: $locationId", it) }
            return
        }
        val activeReminders = reminderDao.getActiveByLocationId(locationId)
        if (activeReminders.isEmpty()) {
            runCatching { geofenceManager.removeGeofences(listOf(locationId)) }
                .onFailure { Log.w(TAG, "Failed to remove geofence for location: $locationId", it) }
        } else {
            val combinedTransition = activeReminders.fold(0) { acc, reminder ->
                acc or reminder.transitionType
            }
            runCatching { geofenceManager.registerGeofenceForLocation(location, combinedTransition) }
                .onFailure { Log.w(TAG, "Failed to register geofence for location: $locationId", it) }
        }
    }

    companion object {
        private const val TAG = "GeotifyRepository"
    }
}
