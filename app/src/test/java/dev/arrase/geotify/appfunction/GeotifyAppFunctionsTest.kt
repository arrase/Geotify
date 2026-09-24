package dev.arrase.geotify.appfunction

import android.location.Location
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.location.LocationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GeotifyAppFunctionsTest {

    companion object {
        @BeforeClass
        @JvmStatic
        @Suppress("DEPRECATION")
        fun initAndroidBundle() {
            try {
                val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
                unsafeField.isAccessible = true
                val unsafe = unsafeField.get(null) as sun.misc.Unsafe
                val emptyField = android.os.Bundle::class.java.getField("EMPTY")
                val base = unsafe.staticFieldBase(emptyField)
                val offset = unsafe.staticFieldOffset(emptyField)
                unsafe.putObject(base, offset, mock<android.os.Bundle>())
            } catch (_: Exception) {
            }
        }
    }

    private val locationRepository: LocationRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val locationProvider: LocationProvider = mock()
    private val appFunctionContext: AppFunctionContext = mock()

    private lateinit var appFunctions: GeotifyAppFunctions

    @Before
    fun setUp() {
        appFunctions = GeotifyAppFunctions(
            locationRepository = locationRepository,
            reminderRepository = reminderRepository,
            locationProvider = locationProvider
        )
    }

    // ── saveCurrentLocation tests ──

    @Test
    fun saveCurrentLocation_whenGpsFixAcquired_savesLocationAndReturnsSuccess() {
        runBlocking {
            val mockLocation: Location = mock()
            whenever(mockLocation.latitude).thenReturn(40.4168)
            whenever(mockLocation.longitude).thenReturn(-3.7038)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(null)
            whenever(locationProvider.getCurrentLocation()).thenReturn(mockLocation)

            val result = appFunctions.saveCurrentLocation(appFunctionContext, "Casa")

            verify(locationRepository).saveLocation("Casa", 40.4168, -3.7038)
            assertEquals("Casa", result.alias)
            assertEquals("Location successfully saved.", result.status)
        }
    }

    @Test
    fun saveCurrentLocation_whenLocationAlreadyExists_throwsAppFunctionInvalidArgumentException() {
        runBlocking {
            val existing = LocationEntity("id-1", "Casa", 40.0, -3.0)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(existing)

            val exception = assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.saveCurrentLocation(appFunctionContext, "Casa")
                }
            }

            assertTrue(exception.message!!.contains("Location alias 'Casa' already exists"))
            verify(locationRepository, never()).saveLocation(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun saveCurrentLocation_whenGpsFixFails_returnsFailureStatus() {
        runBlocking {
            whenever(locationRepository.findLocationByAlias("Trabajo")).thenReturn(null)
            whenever(locationProvider.getCurrentLocation()).thenReturn(null)

            val result = appFunctions.saveCurrentLocation(appFunctionContext, "Trabajo")

            verify(locationRepository, never()).saveLocation(any(), any(), any(), any(), any())
            assertEquals("Trabajo", result.alias)
            assertEquals("Failed to obtain GPS fix.", result.status)
        }
    }

    @Test
    fun saveCurrentLocation_whenLocationProviderThrows_returnsErrorStatus() {
        runBlocking {
            whenever(locationRepository.findLocationByAlias("Gym")).thenReturn(null)
            whenever(locationProvider.getCurrentLocation()).thenThrow(RuntimeException("GPS hardware unavailable"))

            val result = appFunctions.saveCurrentLocation(appFunctionContext, "Gym")

            verify(locationRepository, never()).saveLocation(any(), any(), any(), any(), any())
            assertEquals("Gym", result.alias)
            assertEquals("Error: GPS hardware unavailable", result.status)
        }
    }

    // ── createGeofenceReminder tests ──

    @Test
    fun createGeofenceReminder_withArrivalTrigger_createsReminderWithArrivalType() {
        runBlocking {
            val location = LocationEntity("loc-1", "Super", 40.0, -3.0)
            val createdReminder = ReminderEntity(
                id = "rem-1",
                locationId = "loc-1",
                message = "Comprar leche",
                transitionType = Geofence.GEOFENCE_TRANSITION_ENTER,
                createdAt = 1000L
            )
            whenever(locationRepository.findLocationByAlias("Super")).thenReturn(location)
            whenever(reminderRepository.createReminder(location, "Comprar leche", Geofence.GEOFENCE_TRANSITION_ENTER))
                .thenReturn(createdReminder)

            val result = appFunctions.createGeofenceReminder(
                appFunctionContext = appFunctionContext,
                targetAlias = "Super",
                payloadMessage = "Comprar leche",
                triggerOnArrival = true
            )

            assertEquals("rem-1", result.reminderId)
            assertEquals("Super", result.targetAlias)
            assertEquals("Comprar leche", result.payloadMessage)
            assertEquals("arrival", result.triggerType)
        }
    }

    @Test
    fun createGeofenceReminder_withDepartureTrigger_createsReminderWithDepartureType() {
        runBlocking {
            val location = LocationEntity("loc-1", "Oficina", 40.0, -3.0)
            val createdReminder = ReminderEntity(
                id = "rem-2",
                locationId = "loc-1",
                message = "Cerrar ventana",
                transitionType = Geofence.GEOFENCE_TRANSITION_EXIT,
                createdAt = 2000L
            )
            whenever(locationRepository.findLocationByAlias("Oficina")).thenReturn(location)
            whenever(reminderRepository.createReminder(location, "Cerrar ventana", Geofence.GEOFENCE_TRANSITION_EXIT))
                .thenReturn(createdReminder)

            val result = appFunctions.createGeofenceReminder(
                appFunctionContext = appFunctionContext,
                targetAlias = "Oficina",
                payloadMessage = "Cerrar ventana",
                triggerOnArrival = false
            )

            assertEquals("rem-2", result.reminderId)
            assertEquals("Oficina", result.targetAlias)
            assertEquals("Cerrar ventana", result.payloadMessage)
            assertEquals("departure", result.triggerType)
        }
    }

    @Test
    fun createGeofenceReminder_whenAliasNotFoundAndNoLocationsSaved_throwsExceptionWithSpecificMessage() {
        runBlocking {
            whenever(locationRepository.findLocationByAlias("Desconocido")).thenReturn(null)
            whenever(locationRepository.getAllAliases()).thenReturn(emptyList())

            val exception = assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.createGeofenceReminder(appFunctionContext, "Desconocido", "Test", true)
                }
            }

            assertTrue(exception.message!!.contains("No locations are saved yet"))
            verify(reminderRepository, never()).createReminder(any(), any(), any())
        }
    }

    @Test
    fun createGeofenceReminder_whenAliasNotFoundAndLocationsExist_throwsExceptionWithAvailableAliases() {
        runBlocking {
            whenever(locationRepository.findLocationByAlias("Desconocido")).thenReturn(null)
            whenever(locationRepository.getAllAliases()).thenReturn(listOf("Casa", "Trabajo"))

            val exception = assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.createGeofenceReminder(appFunctionContext, "Desconocido", "Test", true)
                }
            }

            assertTrue(exception.message!!.contains("Valid locations are: Casa, Trabajo"))
            verify(reminderRepository, never()).createReminder(any(), any(), any())
        }
    }

    @Test
    fun createGeofenceReminder_whenRepositoryThrowsIllegalArgumentException_propagatesException() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)
            whenever(reminderRepository.createReminder(location, "", Geofence.GEOFENCE_TRANSITION_ENTER))
                .thenThrow(IllegalArgumentException("Reminder message cannot be blank"))

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    appFunctions.createGeofenceReminder(appFunctionContext, "Casa", "", true)
                }
            }
        }
    }

    // ── listLocations tests ──

    @Test
    fun listLocations_mapsAllEntitiesToSavedLocations() {
        runBlocking {
            val entities = listOf(
                LocationEntity("1", "Casa", 40.4168, -3.7038),
                LocationEntity("2", "Trabajo", 41.3879, 2.1699)
            )
            whenever(locationRepository.getAllLocations()).thenReturn(entities)

            val result = appFunctions.listLocations(appFunctionContext)

            assertEquals(2, result.size)
            assertEquals(SavedLocation("Casa", 40.4168, -3.7038), result[0])
            assertEquals(SavedLocation("Trabajo", 41.3879, 2.1699), result[1])
        }
    }

    @Test
    fun listLocations_whenNoLocationsSaved_returnsEmptyList() {
        runBlocking {
            whenever(locationRepository.getAllLocations()).thenReturn(emptyList())

            val result = appFunctions.listLocations(appFunctionContext)

            assertTrue(result.isEmpty())
        }
    }

    // ── deleteLocation tests ──

    @Test
    fun deleteLocation_whenLocationExists_deletesAndReturnsResult() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)

            val result = appFunctions.deleteLocation(appFunctionContext, "Casa")

            verify(locationRepository).deleteLocation("Casa")
            assertEquals(DeleteResult("Casa", deleted = true), result)
        }
    }

    @Test
    fun deleteLocation_whenLocationNotFound_throwsException() {
        runBlocking {
            whenever(locationRepository.findLocationByAlias("NoExiste")).thenReturn(null)
            whenever(locationRepository.getAllAliases()).thenReturn(emptyList())

            assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.deleteLocation(appFunctionContext, "NoExiste")
                }
            }
            verify(locationRepository, never()).deleteLocation(any())
        }
    }

    // ── deleteReminder (cancelReminder) tests ──

    @Test
    fun deleteReminder_whenSingleReminderAndDefaultMessage_deletesReminder() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            val reminder = ReminderEntity("rem-1", "loc-1", "Comprar pan", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)
            whenever(reminderRepository.getActiveReminders()).thenReturn(listOf(reminder))

            val result = appFunctions.deleteReminder(appFunctionContext, "Casa")

            verify(reminderRepository).cancelReminder("rem-1")
            assertEquals(DeleteResult("Casa", deleted = true), result)
        }
    }

    @Test
    fun deleteReminder_whenLocationNotFound_throwsException() {
        runBlocking {
            whenever(locationRepository.findLocationByAlias("Missing")).thenReturn(null)
            whenever(locationRepository.getAllAliases()).thenReturn(emptyList())

            assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.deleteReminder(appFunctionContext, "Missing", null)
                }
            }
            verify(reminderRepository, never()).cancelReminder(any())
        }
    }

    @Test
    fun deleteReminder_whenNoActiveRemindersForLocation_throwsException() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            val otherReminder = ReminderEntity("rem-2", "loc-other", "Otro", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)
            whenever(reminderRepository.getActiveReminders()).thenReturn(listOf(otherReminder))

            val exception = assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.deleteReminder(appFunctionContext, "Casa", null)
                }
            }

            assertTrue(exception.message!!.contains("No active reminders found for 'Casa'"))
            verify(reminderRepository, never()).cancelReminder(any())
        }
    }

    @Test
    fun deleteReminder_whenMultipleRemindersAndMessageNull_throwsExceptionRequiringMessage() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            val reminder1 = ReminderEntity("rem-1", "loc-1", "Comprar pan", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            val reminder2 = ReminderEntity("rem-2", "loc-1", "Regar plantas", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 200L)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)
            whenever(reminderRepository.getActiveReminders()).thenReturn(listOf(reminder1, reminder2))

            val exception = assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.deleteReminder(appFunctionContext, "Casa", "")
                }
            }

            assertTrue(exception.message!!.contains("Multiple active reminders found for 'Casa'"))
            verify(reminderRepository, never()).cancelReminder(any())
        }
    }

    @Test
    fun deleteReminder_whenMultipleRemindersAndMessageMatches_deletesMatchingReminder() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            val reminder1 = ReminderEntity("rem-1", "loc-1", "Comprar pan", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            val reminder2 = ReminderEntity("rem-2", "loc-1", "Regar plantas", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 200L)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)
            whenever(reminderRepository.getActiveReminders()).thenReturn(listOf(reminder1, reminder2))

            val result = appFunctions.deleteReminder(appFunctionContext, "Casa", "regar")

            verify(reminderRepository).cancelReminder("rem-2")
            assertEquals(DeleteResult("Casa", deleted = true), result)
        }
    }

    @Test
    fun deleteReminder_whenMessageDoesNotMatchAnyReminder_throwsException() {
        runBlocking {
            val location = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            val reminder1 = ReminderEntity("rem-1", "loc-1", "Comprar pan", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            whenever(locationRepository.findLocationByAlias("Casa")).thenReturn(location)
            whenever(reminderRepository.getActiveReminders()).thenReturn(listOf(reminder1))

            val exception = assertThrows(AppFunctionInvalidArgumentException::class.java) {
                runBlocking {
                    appFunctions.deleteReminder(appFunctionContext, "Casa", "Lavar coche")
                }
            }

            assertTrue(exception.message!!.contains("No active reminder matching 'Lavar coche'"))
            verify(reminderRepository, never()).cancelReminder(any())
        }
    }

    // ── listActiveReminders tests ──

    @Test
    fun listActiveReminders_mapsToSavedReminderWithCorrectTriggerTypeAndLocationAlias() {
        runBlocking {
            val loc1 = LocationEntity("loc-1", "Casa", 40.0, -3.0)
            val loc2 = LocationEntity("loc-2", "Trabajo", 41.0, 2.0)
            val reminder1 = ReminderEntity("rem-1", "loc-1", "Pan", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            val reminder2 = ReminderEntity("rem-2", "loc-2", "Reunión", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 200L)
            val reminder3 = ReminderEntity("rem-3", "loc-deleted", "Huérfano", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 300L)

            whenever(reminderRepository.getActiveReminders()).thenReturn(listOf(reminder1, reminder2, reminder3))
            whenever(locationRepository.getAllLocations()).thenReturn(listOf(loc1, loc2))

            val result = appFunctions.listActiveReminders(appFunctionContext)

            assertEquals(3, result.size)
            assertEquals(
                SavedReminder("rem-1", "Casa", "Pan", "arrival"),
                result[0]
            )
            assertEquals(
                SavedReminder("rem-2", "Trabajo", "Reunión", "departure"),
                result[1]
            )
            assertEquals(
                SavedReminder("rem-3", "Unknown", "Huérfano", "arrival"),
                result[2]
            )
        }
    }

    // ── AppFunctionSerializable Data Classes tests ──

    @Test
    fun testSerializableDataClasses() {
        val saveResult1 = SaveLocationResult("Casa", "OK")
        val saveResult2 = SaveLocationResult("Casa", "OK")
        val saveResult3 = saveResult1.copy(status = "Error")
        assertEquals(saveResult1, saveResult2)
        assertNotEquals(saveResult1, saveResult3)
        assertEquals(saveResult1.hashCode(), saveResult2.hashCode())
        val (saveAlias, saveStatus) = saveResult1
        assertEquals("Casa", saveAlias)
        assertEquals("OK", saveStatus)

        val createResult1 = CreateReminderResult("r1", "Casa", "Msg", "arrival")
        val createResult2 = CreateReminderResult("r1", "Casa", "Msg", "arrival")
        val createResult3 = createResult1.copy(triggerType = "departure")
        assertEquals(createResult1, createResult2)
        assertNotEquals(createResult1, createResult3)
        assertEquals("r1", createResult1.reminderId)
        assertEquals("Casa", createResult1.targetAlias)
        assertEquals("Msg", createResult1.payloadMessage)
        assertEquals("arrival", createResult1.triggerType)
        val (cId, cAlias, cMsg, cType) = createResult1
        assertEquals("r1", cId)
        assertEquals("Casa", cAlias)
        assertEquals("Msg", cMsg)
        assertEquals("arrival", cType)

        val savedLoc1 = SavedLocation("Casa", 40.0, -3.0)
        val savedLoc2 = SavedLocation("Casa", 40.0, -3.0)
        val savedLoc3 = savedLoc1.copy(alias = "Trabajo")
        assertEquals(savedLoc1, savedLoc2)
        assertNotEquals(savedLoc1, savedLoc3)
        assertEquals(40.0, savedLoc1.latitude, 0.001)
        assertEquals(-3.0, savedLoc1.longitude, 0.001)
        val (lAlias, lLat, lLng) = savedLoc1
        assertEquals("Casa", lAlias)
        assertEquals(40.0, lLat, 0.001)
        assertEquals(-3.0, lLng, 0.001)

        val deleteResult1 = DeleteResult("Casa", true)
        val deleteResult2 = DeleteResult("Casa", true)
        val deleteResult3 = deleteResult1.copy(deleted = false)
        assertEquals(deleteResult1, deleteResult2)
        assertNotEquals(deleteResult1, deleteResult3)
        assertTrue(deleteResult1.deleted)
        assertFalse(deleteResult3.deleted)
        val (dAlias, dDeleted) = deleteResult1
        assertEquals("Casa", dAlias)
        assertTrue(dDeleted)

        val savedRem1 = SavedReminder("r1", "Casa", "Msg", "arrival")
        val savedRem2 = SavedReminder("r1", "Casa", "Msg", "arrival")
        val savedRem3 = savedRem1.copy(payloadMessage = "New")
        assertEquals(savedRem1, savedRem2)
        assertNotEquals(savedRem1, savedRem3)
        val (srId, srAlias, srMsg, srType) = savedRem1
        assertEquals("r1", srId)
        assertEquals("Casa", srAlias)
        assertEquals("Msg", srMsg)
        assertEquals("arrival", srType)
    }
}
