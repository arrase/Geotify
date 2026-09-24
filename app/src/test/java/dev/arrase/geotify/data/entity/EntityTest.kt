package dev.arrase.geotify.data.entity

import com.google.android.gms.location.Geofence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityTest {

    @Test
    fun locationEntity_defaultValues() {
        val location = LocationEntity(
            id = "loc-1",
            alias = "Home",
            latitude = 40.4168,
            longitude = -3.7038
        )

        assertEquals("loc-1", location.id)
        assertEquals("Home", location.alias)
        assertEquals(40.4168, location.latitude, 0.0001)
        assertEquals(-3.7038, location.longitude, 0.0001)
        assertEquals(150f, location.radiusMeters, 0.0001f)
        assertEquals(0, location.notificationResponsivenessMs)
    }

    @Test
    fun locationEntity_customValues() {
        val location = LocationEntity(
            id = "loc-2",
            alias = "Work",
            latitude = 41.3879,
            longitude = 2.1699,
            radiusMeters = 300f,
            notificationResponsivenessMs = 5000
        )

        assertEquals("loc-2", location.id)
        assertEquals("Work", location.alias)
        assertEquals(41.3879, location.latitude, 0.0001)
        assertEquals(2.1699, location.longitude, 0.0001)
        assertEquals(300f, location.radiusMeters, 0.0001f)
        assertEquals(5000, location.notificationResponsivenessMs)
    }

    @Test
    fun locationEntity_equalityAndCopy() {
        val loc1 = LocationEntity("1", "A", 10.0, 20.0, 100f, 0)
        val loc2 = LocationEntity("1", "A", 10.0, 20.0, 100f, 0)
        val loc3 = loc1.copy(alias = "B")

        assertEquals(loc1, loc2)
        assertEquals(loc1.hashCode(), loc2.hashCode())
        assertNotEquals(loc1, loc3)
        assertEquals("B", loc3.alias)
    }

    @Test
    fun reminderEntity_defaultValues() {
        val reminder = ReminderEntity(
            id = "rem-1",
            locationId = "loc-1",
            message = "Remember keys",
            transitionType = Geofence.GEOFENCE_TRANSITION_ENTER,
            createdAt = 123456789L
        )

        assertEquals("rem-1", reminder.id)
        assertEquals("loc-1", reminder.locationId)
        assertEquals("Remember keys", reminder.message)
        assertEquals(Geofence.GEOFENCE_TRANSITION_ENTER, reminder.transitionType)
        assertEquals(123456789L, reminder.createdAt)
        assertTrue(reminder.isActive)
        assertFalse(reminder.isInRange)
    }

    @Test
    fun reminderEntity_customValues() {
        val reminder = ReminderEntity(
            id = "rem-2",
            locationId = "loc-1",
            message = "Leave keys",
            transitionType = Geofence.GEOFENCE_TRANSITION_EXIT,
            isActive = false,
            createdAt = 987654321L,
            isInRange = true
        )

        assertFalse(reminder.isActive)
        assertTrue(reminder.isInRange)
    }

    @Test
    fun reminderEntity_equalityAndCopy() {
        val rem1 = ReminderEntity("1", "loc-1", "Msg", Geofence.GEOFENCE_TRANSITION_ENTER, true, 100L, false)
        val rem2 = ReminderEntity("1", "loc-1", "Msg", Geofence.GEOFENCE_TRANSITION_ENTER, true, 100L, false)
        val rem3 = rem1.copy(isActive = false)

        assertEquals(rem1, rem2)
        assertEquals(rem1.hashCode(), rem2.hashCode())
        assertNotEquals(rem1, rem3)
        assertFalse(rem3.isActive)
    }

    @Test
    fun reminderEntity_isArrival_whenEnterTransition() {
        val reminder = ReminderEntity(
            id = "1",
            locationId = "loc-1",
            message = "Welcome",
            transitionType = Geofence.GEOFENCE_TRANSITION_ENTER,
            createdAt = 100L
        )

        assertTrue(reminder.isArrival)
        assertFalse(reminder.isDeparture)
        assertEquals("arrival", reminder.triggerTypeString)
    }

    @Test
    fun reminderEntity_isDeparture_whenExitTransition() {
        val reminder = ReminderEntity(
            id = "2",
            locationId = "loc-1",
            message = "Goodbye",
            transitionType = Geofence.GEOFENCE_TRANSITION_EXIT,
            createdAt = 100L
        )

        assertFalse(reminder.isArrival)
        assertTrue(reminder.isDeparture)
        assertEquals("departure", reminder.triggerTypeString)
    }

    @Test
    fun reminderEntity_triggerTypeString_whenOtherTransition() {
        val reminder = ReminderEntity(
            id = "3",
            locationId = "loc-1",
            message = "Dwell",
            transitionType = Geofence.GEOFENCE_TRANSITION_DWELL,
            createdAt = 100L
        )

        assertFalse(reminder.isArrival)
        assertFalse(reminder.isDeparture)
        assertEquals("departure", reminder.triggerTypeString)
    }

    @Test
    fun locationReminderCount_propertiesAndEquality() {
        val count1 = LocationReminderCount("loc-1", 5)
        val count2 = LocationReminderCount("loc-1", 5)
        val count3 = count1.copy(count = 10)

        assertEquals("loc-1", count1.locationId)
        assertEquals(5, count1.count)
        assertEquals(count1, count2)
        assertEquals(count1.hashCode(), count2.hashCode())
        assertNotEquals(count1, count3)
        assertEquals(10, count3.count)
    }
}
