package dev.arrase.geotify.data

import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.dao.ReminderDao
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.domain.SpatialSearchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The app never blocks reminder creation; instead the geofence window registered with Play services
 * is capped, and only the closest locations are monitored. These tests pin that budget down so it
 * cannot drift past the GMS limit of 100 geofences per app.
 */
class GeofenceLimitTest {

    private val reminderDao: ReminderDao = mock()
    private val repository = ReminderRepository(reminderDao, Dispatchers.Unconfined)

    private val locations = (1..150).map { index ->
        LocationEntity(
            id = "loc_$index",
            alias = "Location $index",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 100f
        )
    }

    @Test
    fun `geofence budget leaves exactly one slot for the master geofence`() {
        assertEquals(100, SpatialSearchUseCase.MAX_POI_GEOFENCES + 1)
    }

    @Test
    fun `creating more reminders than the budget is allowed and tracked`() = runBlocking {
        val created = locations.mapIndexed { index, location ->
            repository.createReminder(location, "Reminder $index", Geofence.GEOFENCE_TRANSITION_ENTER)
        }

        assertEquals(150, created.size)
        assertTrue(created.size > SpatialSearchUseCase.MAX_POI_GEOFENCES)
        created.forEach { verify(reminderDao).insert(it) }
    }

    @Test
    fun `deactivating and reactivating a reminder round-trips its state`() = runBlocking {
        val reminder = repository.createReminder(
            locations.first(),
            "Buy milk",
            Geofence.GEOFENCE_TRANSITION_ENTER
        )

        repository.deactivateReminder(reminder.id)
        verify(reminderDao).deactivate(reminder.id)

        val reactivated: ReminderEntity = reminder.copy(isActive = true)
        repository.updateReminder(reactivated)
        verify(reminderDao).update(reactivated)
    }

    @Test
    fun `getActiveReminders returns exactly what the dao reports`() = runBlocking {
        val active = listOf(ReminderEntity("r1", "loc_1", "Msg", 1, true, 0L))
        whenever(reminderDao.getActiveReminders()).thenReturn(active)

        assertEquals(active, repository.getActiveReminders())
    }
}
