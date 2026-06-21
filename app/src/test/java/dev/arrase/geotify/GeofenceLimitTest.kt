package dev.arrase.geotify

import android.content.Context
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeofenceLimitTest {

    private val locationsList = mutableListOf<LocationEntity>()
    private val remindersList = mutableListOf<ReminderEntity>()

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

    private val reminderRepository = ReminderRepository(
        fakeReminderDao,
        kotlinx.coroutines.Dispatchers.Unconfined
    )

    @Test
    fun testCreateManyRemindersWithoutLimit() = runBlocking {
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

        for (i in 0 until 150) {
            val reminder = reminderRepository.createReminder(locations[i], "Reminder $i", 1)
            assertNotNull(reminder)
        }
        assertEquals(150, fakeReminderDao.getActiveGeofenceCount())
    }

    @Test
    fun testDeactivateAndUpdateReminder() = runBlocking {
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
            reminderRepository.createReminder(locations[i], "Reminder $i", 1)
        }

        val reminder150 = reminderRepository.createReminder(locations[149], "Reminder 150", 1)
        assertEquals(150, fakeReminderDao.getActiveGeofenceCount())

        reminderRepository.deactivateReminder(reminder150.id)
        assertEquals(149, fakeReminderDao.getActiveGeofenceCount())

        reminderRepository.updateReminder(reminder150.copy(isActive = true))
        assertEquals(150, fakeReminderDao.getActiveGeofenceCount())
    }
}
