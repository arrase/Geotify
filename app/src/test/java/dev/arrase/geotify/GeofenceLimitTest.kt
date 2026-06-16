package dev.arrase.geotify

import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.GeofenceLimitExceededException
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
    }

    private val fakeGeofenceManager = object : GeofenceManager {
        override suspend fun registerGeofenceForLocation(location: LocationEntity, transitionTypes: Int) {}
        override suspend fun removeGeofences(requestIds: List<String>) {}
    }

    private val repository = GeotifyRepository(
        fakeLocationDao,
        fakeReminderDao,
        fakeGeofenceManager,
        kotlinx.coroutines.Dispatchers.Unconfined
    )

    @Test
    fun testGeofenceLimitEnforcement() = runBlocking {
        locationsList.clear()
        remindersList.clear()

        val locations = (1..101).map { i ->
            LocationEntity(
                id = "loc_$i",
                alias = "Location $i",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 100f
            ).also { locationsList.add(it) }
        }

        for (i in 0 until 99) {
            val result = repository.createReminder(locations[i], "Reminder $i", 1)
            assertFalse(result.isLimitWarningTriggered)
        }
        assertEquals(99, fakeReminderDao.getActiveGeofenceCount())

        val result100 = repository.createReminder(locations[99], "Reminder 100", 1)
        assertTrue(result100.isLimitWarningTriggered)
        assertEquals(100, fakeReminderDao.getActiveGeofenceCount())

        val resultExisting = repository.createReminder(locations[0], "Another Reminder in Location 1", 1)
        assertFalse(resultExisting.isLimitWarningTriggered)
        assertEquals(100, fakeReminderDao.getActiveGeofenceCount())

        try {
            repository.createReminder(locations[100], "Reminder 101", 1)
            fail("Expected GeofenceLimitExceededException to be thrown")
        } catch (e: GeofenceLimitExceededException) {
            assertEquals("Geofence limit reached (maximum 100).", e.message)
        }
        assertEquals(100, fakeReminderDao.getActiveGeofenceCount())
    }

    @Test
    fun testGeofenceLimitOnUpdateReminder() = runBlocking {
        locationsList.clear()
        remindersList.clear()

        val locations = (1..101).map { i ->
            LocationEntity(
                id = "loc_$i",
                alias = "Location $i",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 100f
            ).also { locationsList.add(it) }
        }

        for (i in 0 until 99) {
            repository.createReminder(locations[i], "Reminder $i", 1)
        }

        val reminder100 = repository.createReminder(locations[99], "Reminder 100", 1).reminder
        assertEquals(100, fakeReminderDao.getActiveGeofenceCount())

        repository.deactivateReminder(reminder100.id)
        assertEquals(99, fakeReminderDao.getActiveGeofenceCount())

        val warningTriggered = repository.updateReminder(reminder100.copy(isActive = true), reminder100.locationId)
        assertTrue(warningTriggered)
        assertEquals(100, fakeReminderDao.getActiveGeofenceCount())

        val reminder101 = repository.createReminder(locations[0], "Temp reminder", 1).reminder
        repository.deactivateReminder(reminder101.id)

        try {
            repository.updateReminder(
                reminder101.copy(locationId = locations[100].id, isActive = true),
                oldLocationId = reminder101.locationId
            )
            fail("Expected GeofenceLimitExceededException")
        } catch (e: GeofenceLimitExceededException) {
            assertEquals("Geofence limit reached (maximum 100).", e.message)
        }
    }
}
