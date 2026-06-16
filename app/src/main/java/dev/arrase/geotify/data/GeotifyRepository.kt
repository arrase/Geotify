package dev.arrase.geotify.data

import android.util.Log
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.di.IoDispatcher
import dev.arrase.geotify.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class GeofenceLimitExceededException(message: String) : Exception(message)

data class ReminderCreationResult(
    val reminder: ReminderEntity,
    val isLimitWarningTriggered: Boolean = false
)

@Singleton
class GeotifyRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val reminderDao: ReminderDao,
    private val geofenceManager: GeofenceManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
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
    ): LocationEntity = withContext(ioDispatcher) {
        require(latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        require(radiusMeters >= 50f) { "Geofence radius must be at least 50 meters" }
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            alias = alias,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters
        )
        locationDao.insert(entity)
        return@withContext entity
    }

    suspend fun updateLocation(location: LocationEntity) = withContext(ioDispatcher) {
        require(location.latitude in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(location.longitude in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        require(location.radiusMeters >= 50f) { "Geofence radius must be at least 50 meters" }
        locationDao.update(location)
        syncGeofenceForLocation(location.id)
    }

    suspend fun getAllLocations(): List<LocationEntity> = withContext(ioDispatcher) {
        locationDao.getAll()
    }

    suspend fun findLocationByAlias(alias: String): LocationEntity? = withContext(ioDispatcher) {
        locationDao.findByAlias(alias)
    }

    suspend fun getAllAliases(): List<String> = withContext(ioDispatcher) {
        locationDao.getAllAliases()
    }

    suspend fun deleteLocation(alias: String) = withContext(ioDispatcher) {
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
    ): ReminderCreationResult = withContext(ioDispatcher) {
        val warningTriggered = checkGeofenceLimitForFutureState(null, location.id, true)
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            locationId = location.id,
            message = message,
            transitionType = transitionType,
            createdAt = System.currentTimeMillis()
        )
        reminderDao.insert(reminder)
        syncGeofenceForLocation(location.id)
        return@withContext ReminderCreationResult(reminder, warningTriggered)
    }

    suspend fun updateReminder(reminder: ReminderEntity, oldLocationId: String): Boolean = withContext(ioDispatcher) {
        val warningTriggered = checkGeofenceLimitForFutureState(reminder.id, reminder.locationId, reminder.isActive)
        reminderDao.update(reminder)
        syncGeofenceForLocation(reminder.locationId)
        if (oldLocationId != reminder.locationId) {
            syncGeofenceForLocation(oldLocationId)
        }
        return@withContext warningTriggered
    }

    private suspend fun checkGeofenceLimitForFutureState(
        modifiedReminderId: String?,
        newLocationId: String,
        newIsActive: Boolean
    ): Boolean {
        val activeReminders = reminderDao.getActiveReminders()
        val currentActiveGeofences = activeReminders.map { it.locationId }.distinct().size

        val activeLocations = activeReminders
            .filter { it.id != modifiedReminderId }
            .map { it.locationId }
            .toMutableSet()
        if (newIsActive) {
            activeLocations.add(newLocationId)
        }

        val futureActiveGeofences = activeLocations.size
        if (futureActiveGeofences > 100) {
            throw GeofenceLimitExceededException("Geofence limit reached (maximum 100).")
        }
        return futureActiveGeofences == 100 && currentActiveGeofences < 100
    }

    suspend fun deactivateReminder(reminderId: String) = withContext(ioDispatcher) {
        val reminder = reminderDao.findById(reminderId) ?: return@withContext
        reminderDao.deactivate(reminderId)
        syncGeofenceForLocation(reminder.locationId)
    }

    suspend fun cancelReminder(reminderId: String) = withContext(ioDispatcher) {
        val reminder = reminderDao.findById(reminderId) ?: return@withContext
        reminderDao.deleteById(reminderId)
        syncGeofenceForLocation(reminder.locationId)
    }

    suspend fun findReminderById(id: String): ReminderEntity? = withContext(ioDispatcher) {
        reminderDao.findById(id)
    }

    suspend fun findLocationById(id: String): LocationEntity? = withContext(ioDispatcher) {
        locationDao.findById(id)
    }

    suspend fun getActiveReminders(): List<ReminderEntity> = withContext(ioDispatcher) {
        reminderDao.getActiveReminders()
    }

    suspend fun getActiveRemindersForLocation(locationId: String): List<ReminderEntity> = withContext(ioDispatcher) {
        reminderDao.getActiveByLocationId(locationId)
    }

    // ── Geofence Re-registration (after boot) ──

    suspend fun reRegisterAllActiveGeofences() = withContext(ioDispatcher) {
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
