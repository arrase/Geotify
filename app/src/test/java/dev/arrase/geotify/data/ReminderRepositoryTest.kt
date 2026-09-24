package dev.arrase.geotify.data

import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ReminderRepositoryTest {

    private val reminderDao: ReminderDao = mock()
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        repository = ReminderRepository(
            reminderDao = reminderDao,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun createReminder_insertsAndReturnsReminder() {
        runBlocking {
            val location = LocationEntity(
                id = "loc-abc",
                alias = "Supermarket",
                latitude = 40.0,
                longitude = -3.0
            )

            val beforeTime = System.currentTimeMillis()
            val result = repository.createReminder(
                location = location,
                message = "Buy milk",
                transitionType = 1
            )
            val afterTime = System.currentTimeMillis()

            assertNotNull(result.id)
            assertEquals("loc-abc", result.locationId)
            assertEquals("Buy milk", result.message)
            assertEquals(1, result.transitionType)
            assertTrue(result.isActive)
            assertTrue(result.createdAt in beforeTime..afterTime)
            verify(reminderDao).insert(result)
        }
    }

    @Test
    fun updateReminder_callsDaoUpdate() {
        runBlocking {
            val reminder = ReminderEntity(
                id = "rem-1",
                locationId = "loc-1",
                message = "Buy bread",
                transitionType = 2,
                createdAt = 1000L
            )

            repository.updateReminder(reminder)

            verify(reminderDao).update(reminder)
        }
    }

    @Test
    fun deactivateReminder_callsDaoDeactivate() {
        runBlocking {
            repository.deactivateReminder("rem-123")

            verify(reminderDao).deactivate("rem-123")
        }
    }

    @Test
    fun cancelReminder_callsDaoDeleteById() {
        runBlocking {
            repository.cancelReminder("rem-456")

            verify(reminderDao).deleteById("rem-456")
        }
    }

    @Test
    fun getActiveReminders_returnsRemindersFromDao() {
        runBlocking {
            val activeList = listOf(
                ReminderEntity("1", "loc-1", "Msg 1", 1, true, 100L),
                ReminderEntity("2", "loc-2", "Msg 2", 2, true, 200L)
            )
            whenever(reminderDao.getActiveReminders()).thenReturn(activeList)

            val result = repository.getActiveReminders()

            assertEquals(activeList, result)
            verify(reminderDao).getActiveReminders()
        }
    }

    @Test
    fun getActiveRemindersForLocation_returnsMatchingRemindersFromDao() {
        runBlocking {
            val reminders = listOf(
                ReminderEntity("1", "loc-1", "Msg 1", 1, true, 100L)
            )
            whenever(reminderDao.getActiveByLocationId("loc-1")).thenReturn(reminders)

            val result = repository.getActiveRemindersForLocation("loc-1")

            assertEquals(reminders, result)
            verify(reminderDao).getActiveByLocationId("loc-1")
        }
    }

    @Test
    fun updateInRangeStatus_callsDaoUpdateInRangeStatus() {
        runBlocking {
            val locationIds = listOf("loc-1", "loc-2")

            repository.updateInRangeStatus(locationIds)

            verify(reminderDao).updateInRangeStatus(locationIds)
        }
    }

    @Test
    fun observeReminders_delegatesToDaoObserveAll() {
        runBlocking {
            val reminders = listOf(
                ReminderEntity("1", "loc-1", "Msg", 1, true, 50L)
            )
            whenever(reminderDao.observeAll()).thenReturn(flowOf(reminders))

            val result = repository.observeReminders().first()

            assertEquals(reminders, result)
            verify(reminderDao).observeAll()
        }
    }

    @Test
    fun observeActiveReminderCounts_delegatesToDao() {
        runBlocking {
            val counts = listOf(
                LocationReminderCount(locationId = "loc-1", count = 3),
                LocationReminderCount(locationId = "loc-2", count = 1)
            )
            whenever(reminderDao.observeActiveReminderCounts()).thenReturn(flowOf(counts))

            val result = repository.observeActiveReminderCounts().first()

            assertEquals(counts, result)
            verify(reminderDao).observeActiveReminderCounts()
        }
    }
}
