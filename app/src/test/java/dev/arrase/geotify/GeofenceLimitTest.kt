package dev.arrase.geotify

import android.content.Context
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.geofence.GeofenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class GeofenceLimitTest {

    private val locationsList = mutableListOf<LocationEntity>()
    private val remindersList = mutableListOf<ReminderEntity>()

    private val fakeLocationDao = object : LocationDao {
        override fun observeAll(): Flow<List<LocationEntity>> = flowOf(locationsList)
        override suspend fun getAll(): List<LocationEntity> = locationsList
        override suspend fun findByAlias(alias: String): LocationEntity? =
            locationsList.find { it.alias.equals(alias, ignoreCase = true) }
        override suspend fun findById(id: String): LocationEntity? =
            locationsList.find { it.id == id }
        override suspend fun getAllAliases(): List<String> =
            locationsList.map { it.alias }
        override suspend fun insert(location: LocationEntity) {
            locationsList.add(location)
        }
        override suspend fun update(location: LocationEntity) {
            val idx = locationsList.indexOfFirst { it.id == location.id }
            if (idx != -1) locationsList[idx] = location
        }
        override suspend fun deleteByAlias(alias: String): Int {
            val count = locationsList.size
            locationsList.removeAll { it.alias.equals(alias, ignoreCase = true) }
            return count - locationsList.size
        }
        override suspend fun getLocationsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<LocationEntity> = emptyList()
    }

    private val fakeReminderDao = object : ReminderDao {
        override fun observeActiveReminderCounts(): Flow<List<LocationReminderCount>> = flowOf(emptyList())
        override fun observeAll(): Flow<List<ReminderEntity>> = flowOf(remindersList)
        override suspend fun getActiveReminders(): List<ReminderEntity> =
            remindersList.filter { it.isActive }
        override suspend fun findById(id: String): ReminderEntity? =
            remindersList.find { it.id == id }
        override suspend fun getActiveByLocationId(locationId: String): List<ReminderEntity> =
            remindersList.filter { it.locationId == locationId && it.isActive }
        override fun observeActiveCountForLocation(locationId: String): Flow<Int> = flowOf(0)
        override suspend fun insert(reminder: ReminderEntity) {
            remindersList.add(reminder)
        }
        override suspend fun update(reminder: ReminderEntity) {
            val idx = remindersList.indexOfFirst { it.id == reminder.id }
            if (idx != -1) remindersList[idx] = reminder
        }
        override suspend fun deactivate(id: String) {
            val idx = remindersList.indexOfFirst { it.id == id }
            if (idx != -1) {
                remindersList[idx] = remindersList[idx].copy(isActive = false)
            }
        }
        override suspend fun deleteById(id: String): Int {
            val count = remindersList.size
            remindersList.removeAll { it.id == id }
            return count - remindersList.size
        }
        override suspend fun getActiveReminderIdsByAlias(alias: String): List<String> = emptyList()
        override suspend fun getActiveGeofenceCount(): Int =
            remindersList.filter { it.isActive }.map { it.locationId }.distinct().size
        override suspend fun clearAllInRange() {}
        override suspend fun setInRangeForLocations(locationIds: List<String>) {}
    }

    private val fakeGeofenceManager = object : GeofenceManager {
        override suspend fun registerGeofenceForLocation(location: LocationEntity, transitionTypes: Int) {}
        override suspend fun removeGeofences(requestIds: List<String>) {}
        override suspend fun removeAllGeofences() {}
        override suspend fun registerSlidingWindowGeofences(locations: List<LocationEntity>, centerLat: Double, centerLon: Double, innerRadiusMeters: Float) {}
    }

    private val mockContext = mock(Context::class.java)

    private val repository = GeotifyRepository(
        mockContext,
        fakeLocationDao,
        fakeReminderDao,
        fakeGeofenceManager,
        kotlinx.coroutines.Dispatchers.Unconfined
    )

    @Test
    fun testGeofenceLimitExceededNoLongerEnforced() = runBlocking {
        locationsList.clear()
        remindersList.clear()

        // Create 150 locations and reminders to show limit is bypassed
        val locations = (1..150).map { i ->
            LocationEntity(
                id = "loc_$i",
                alias = "Location $i",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 100f
            ).also { locationsList.add(it) }
        }

        for (i in 0 until 150) {
            val result = repository.createReminder(locations[i], "Reminder $i", 1)
            assertFalse(result.isLimitWarningTriggered)
        }
        assertEquals(150, fakeReminderDao.getActiveGeofenceCount())
    }

    @Test
    fun testGeofenceLimitOnUpdateReminderNoLongerEnforced() = runBlocking {
        locationsList.clear()
        remindersList.clear()

        val locations = (1..150).map { i ->
            LocationEntity(
                id = "loc_$i",
                alias = "Location $i",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 100f
            ).also { locationsList.add(it) }
        }

        for (i in 0 until 149) {
            repository.createReminder(locations[i], "Reminder $i", 1)
        }

        val reminder150 = repository.createReminder(locations[149], "Reminder 150", 1).reminder
        assertEquals(150, fakeReminderDao.getActiveGeofenceCount())

        repository.deactivateReminder(reminder150.id)
        assertEquals(149, fakeReminderDao.getActiveGeofenceCount())

        val warningTriggered = repository.updateReminder(reminder150.copy(isActive = true), reminder150.locationId)
        assertFalse(warningTriggered)
        assertEquals(150, fakeReminderDao.getActiveGeofenceCount())
    }
}
