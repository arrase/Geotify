package dev.arrase.geotify.data

import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.entity.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LocationRepositoryTest {

    private val locationDao: LocationDao = mock()
    private lateinit var repository: LocationRepository

    @Before
    fun setUp() {
        repository = LocationRepository(
            locationDao = locationDao,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun saveLocation_withValidParameters_insertsAndReturnsEntity() {
        runBlocking {
            val result = repository.saveLocation(
                alias = "Home",
                latitude = 40.4168,
                longitude = -3.7038,
                radiusMeters = 150f,
                notificationResponsivenessMs = 1000
            )

            assertNotNull(result.id)
            assertEquals("Home", result.alias)
            assertEquals(40.4168, result.latitude, 0.0001)
            assertEquals(-3.7038, result.longitude, 0.0001)
            assertEquals(150f, result.radiusMeters, 0.0001f)
            assertEquals(1000, result.notificationResponsivenessMs)
            verify(locationDao).insert(result)
        }
    }

    @Test
    fun saveLocation_withDefaultRadiusAndResponsiveness_insertsAndReturnsEntity() {
        runBlocking {
            val result = repository.saveLocation(
                alias = "Work",
                latitude = 41.3879,
                longitude = 2.1699
            )

            assertNotNull(result.id)
            assertEquals("Work", result.alias)
            assertEquals(150f, result.radiusMeters, 0.0001f)
            assertEquals(0, result.notificationResponsivenessMs)
            verify(locationDao).insert(result)
        }
    }

    @Test
    fun saveLocation_withLatitudeBelowMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveLocation("Invalid", -91.0, 0.0)
            }
        }
        runBlocking { verify(locationDao, never()).insert(any()) }
    }

    @Test
    fun saveLocation_withLatitudeAboveMaximum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveLocation("Invalid", 91.0, 0.0)
            }
        }
        runBlocking { verify(locationDao, never()).insert(any()) }
    }

    @Test
    fun saveLocation_withLongitudeBelowMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveLocation("Invalid", 0.0, -181.0)
            }
        }
        runBlocking { verify(locationDao, never()).insert(any()) }
    }

    @Test
    fun saveLocation_withLongitudeAboveMaximum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveLocation("Invalid", 0.0, 181.0)
            }
        }
        runBlocking { verify(locationDao, never()).insert(any()) }
    }

    @Test
    fun saveLocation_withRadiusBelowMinimum_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveLocation("Invalid", 0.0, 0.0, radiusMeters = 49.9f)
            }
        }
        runBlocking { verify(locationDao, never()).insert(any()) }
    }

    @Test
    fun saveLocation_withNegativeResponsiveness_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveLocation("Invalid", 0.0, 0.0, notificationResponsivenessMs = -1)
            }
        }
        runBlocking { verify(locationDao, never()).insert(any()) }
    }

    @Test
    fun updateLocation_withValidLocation_updatesDao() {
        runBlocking {
            val location = LocationEntity(
                id = "loc-1",
                alias = "Home",
                latitude = 40.0,
                longitude = -3.0,
                radiusMeters = 100f,
                notificationResponsivenessMs = 0
            )

            repository.updateLocation(location)

            verify(locationDao).update(location)
        }
    }

    @Test
    fun updateLocation_withInvalidLatitude_throwsIllegalArgumentException() {
        val location = LocationEntity(
            id = "loc-1",
            alias = "Invalid",
            latitude = -95.0,
            longitude = 0.0,
            radiusMeters = 100f
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateLocation(location) }
        }
        runBlocking { verify(locationDao, never()).update(any()) }
    }

    @Test
    fun updateLocation_withInvalidLongitude_throwsIllegalArgumentException() {
        val location = LocationEntity(
            id = "loc-1",
            alias = "Invalid",
            latitude = 0.0,
            longitude = 185.0,
            radiusMeters = 100f
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateLocation(location) }
        }
        runBlocking { verify(locationDao, never()).update(any()) }
    }

    @Test
    fun updateLocation_withInvalidRadius_throwsIllegalArgumentException() {
        val location = LocationEntity(
            id = "loc-1",
            alias = "Invalid",
            latitude = 0.0,
            longitude = 0.0,
            radiusMeters = 40f
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateLocation(location) }
        }
        runBlocking { verify(locationDao, never()).update(any()) }
    }

    @Test
    fun getAllLocations_returnsDaoLocations() {
        runBlocking {
            val locations = listOf(
                LocationEntity("1", "A", 10.0, 20.0),
                LocationEntity("2", "B", 30.0, 40.0)
            )
            whenever(locationDao.getAll()).thenReturn(locations)

            val result = repository.getAllLocations()

            assertEquals(locations, result)
            verify(locationDao).getAll()
        }
    }

    @Test
    fun findLocationByAlias_returnsLocationWhenFound() {
        runBlocking {
            val location = LocationEntity("1", "Office", 10.0, 20.0)
            whenever(locationDao.findByAlias("Office")).thenReturn(location)

            val result = repository.findLocationByAlias("Office")

            assertEquals(location, result)
            verify(locationDao).findByAlias("Office")
        }
    }

    @Test
    fun findLocationByAlias_returnsNullWhenNotFound() {
        runBlocking {
            whenever(locationDao.findByAlias("Unknown")).thenReturn(null)

            val result = repository.findLocationByAlias("Unknown")

            assertNull(result)
            verify(locationDao).findByAlias("Unknown")
        }
    }

    @Test
    fun findLocationById_returnsLocationWhenFound() {
        runBlocking {
            val location = LocationEntity("id-123", "Gym", 10.0, 20.0)
            whenever(locationDao.findById("id-123")).thenReturn(location)

            val result = repository.findLocationById("id-123")

            assertEquals(location, result)
            verify(locationDao).findById("id-123")
        }
    }

    @Test
    fun findLocationById_returnsNullWhenNotFound() {
        runBlocking {
            whenever(locationDao.findById("missing-id")).thenReturn(null)

            val result = repository.findLocationById("missing-id")

            assertNull(result)
            verify(locationDao).findById("missing-id")
        }
    }

    @Test
    fun getAllAliases_returnsAliasesFromDao() {
        runBlocking {
            val aliases = listOf("Casa", "Gimnasio", "Trabajo")
            whenever(locationDao.getAllAliases()).thenReturn(aliases)

            val result = repository.getAllAliases()

            assertEquals(aliases, result)
            verify(locationDao).getAllAliases()
        }
    }

    @Test
    fun deleteLocation_callsDaoDeleteByAlias() {
        runBlocking {
            repository.deleteLocation("OldPlace")

            verify(locationDao).deleteByAlias("OldPlace")
        }
    }

    @Test
    fun observeLocations_delegatesToDaoObserveAll() {
        runBlocking {
            val locations = listOf(LocationEntity("1", "Test", 0.0, 0.0))
            whenever(locationDao.observeAll()).thenReturn(flowOf(locations))

            val result = repository.observeLocations().first()

            assertEquals(locations, result)
            verify(locationDao).observeAll()
        }
    }
}
