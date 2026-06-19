package dev.arrase.geotify.data

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.di.IoDispatcher
import dev.arrase.geotify.geofence.GeofenceManager
import dev.arrase.geotify.geofence.GeofenceRecalculationWorker
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
    @param:ApplicationContext private val context: Context,
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
        triggerRecalculation()
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
            locationDao.deleteByAlias(alias)
            triggerRecalculation()
        }
    }

    // ── Reminder Operations ──

    suspend fun createReminder(
        location: LocationEntity,
        message: String,
        transitionType: Int
    ): ReminderCreationResult = withContext(ioDispatcher) {
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            locationId = location.id,
            message = message,
            transitionType = transitionType,
            createdAt = System.currentTimeMillis()
        )
        reminderDao.insert(reminder)
        syncGeofenceForLocation(location.id)
        return@withContext ReminderCreationResult(reminder, false)
    }

    suspend fun updateReminder(reminder: ReminderEntity, oldLocationId: String): Boolean = withContext(ioDispatcher) {
        reminderDao.update(reminder)
        syncGeofenceForLocation(reminder.locationId)
        if (oldLocationId != reminder.locationId) {
            syncGeofenceForLocation(oldLocationId)
        }
        return@withContext false
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
        Log.i(TAG, "reRegisterAllActiveGeofences: Triggering sliding window geofence recalculation...")
        triggerRecalculation()
    }

    // ── Sync Helper ──

    private fun syncGeofenceForLocation(locationId: String) {
        Log.i(TAG, "syncGeofenceForLocation: Triggering recalculation for change in location $locationId")
        triggerRecalculation()
    }

    fun triggerRecalculation() {
        Log.i(TAG, "triggerRecalculation: Enqueuing GeofenceRecalculationWorker...")
        try {
            val workRequest = OneTimeWorkRequestBuilder<GeofenceRecalculationWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("geofence_recalculation", ExistingWorkPolicy.REPLACE, workRequest)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "WorkManager is not initialized (likely running in a JUnit test environment). Skipping enqueue.")
        }
    }

    companion object {
        private const val TAG = "GeotifyRepository"
    }
}
