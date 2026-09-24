package dev.arrase.geotify.domain

import android.location.Location
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.entity.LocationEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.math.cos

class SpatialSearchUseCaseTest {

    private val locationDao: LocationDao = mock()
    private lateinit var useCase: SpatialSearchUseCase
    private lateinit var mockedLocation: MockedStatic<Location>
    private var distanceCalculator: (Double, Double, Double, Double) -> Float = { _, _, _, _ -> 0f }

    @Before
    fun setUp() {
        useCase = SpatialSearchUseCase(locationDao)
        mockedLocation = mockStatic(Location::class.java)
        mockedLocation.`when`<Any> {
            Location.distanceBetween(
                any(),
                any(),
                any(),
                any(),
                any<FloatArray>()
            )
        }.thenAnswer { invocation ->
            val startLat = invocation.getArgument<Double>(0)
            val startLon = invocation.getArgument<Double>(1)
            val destLat = invocation.getArgument<Double>(2)
            val destLon = invocation.getArgument<Double>(3)
            val buffer = invocation.getArgument<FloatArray>(4)
            buffer[0] = distanceCalculator(startLat, startLon, destLat, destLon)
            null
        }
    }

    @After
    fun tearDown() {
        mockedLocation.close()
    }

    @Test
    fun execute_computesCorrectBoundingBox() = runBlocking {
        val centerLat = 40.0
        val centerLon = -3.0
        val radiusKm = 10f
        val radiusMeters = 10000.0
        val latDegreesChange = radiusMeters / 111320.0
        val cosLat = cos(Math.toRadians(centerLat))
        val lonDegreesChange = radiusMeters / (111320.0 * cosLat)

        val expectedMinLat = centerLat - latDegreesChange
        val expectedMaxLat = centerLat + latDegreesChange
        val expectedMinLon = centerLon - lonDegreesChange
        val expectedMaxLon = centerLon + lonDegreesChange

        whenever(locationDao.getLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(emptyList())

        useCase.execute(centerLat, centerLon, radiusKm)

        val minLatCaptor = ArgumentCaptor.forClass(Double::class.java)
        val maxLatCaptor = ArgumentCaptor.forClass(Double::class.java)
        val minLonCaptor = ArgumentCaptor.forClass(Double::class.java)
        val maxLonCaptor = ArgumentCaptor.forClass(Double::class.java)

        verify(locationDao).getLocationsInBoundingBox(
            minLatCaptor.capture(),
            maxLatCaptor.capture(),
            minLonCaptor.capture(),
            maxLonCaptor.capture()
        )

        assertEquals(expectedMinLat, minLatCaptor.value, 0.0001)
        assertEquals(expectedMaxLat, maxLatCaptor.value, 0.0001)
        assertEquals(expectedMinLon, minLonCaptor.value, 0.0001)
        assertEquals(expectedMaxLon, maxLonCaptor.value, 0.0001)
    }

    @Test
    fun execute_whenCosLatNonPositive_uses360DegreesChange() = runBlocking {
        val centerLat = 100.0
        val centerLon = 10.0
        val radiusKm = 5f

        whenever(locationDao.getLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(emptyList())

        useCase.execute(centerLat, centerLon, radiusKm)

        val minLonCaptor = ArgumentCaptor.forClass(Double::class.java)
        val maxLonCaptor = ArgumentCaptor.forClass(Double::class.java)

        verify(locationDao).getLocationsInBoundingBox(
            any(),
            any(),
            minLonCaptor.capture(),
            maxLonCaptor.capture()
        )

        assertEquals(centerLon - 360.0, minLonCaptor.value, 0.0001)
        assertEquals(centerLon + 360.0, maxLonCaptor.value, 0.0001)
    }

    @Test
    fun execute_filtersCandidatesOutsideRadius_andSortsByDistanceAscending() = runBlocking {
        val candidateClose = LocationEntity("1", "Close", 40.01, -3.01)
        val candidateMedium = LocationEntity("2", "Medium", 40.02, -3.02)
        val candidateFar = LocationEntity("3", "Far", 40.10, -3.10)

        val candidates = listOf(candidateMedium, candidateFar, candidateClose)
        whenever(locationDao.getLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(candidates)

        distanceCalculator = { _, _, destLat, _ ->
            when (destLat) {
                40.01 -> 1000f
                40.02 -> 3000f
                40.10 -> 8000f
                else -> 0f
            }
        }

        val result = useCase.execute(40.0, -3.0, 5.0f)

        assertEquals(2, result.size)
        assertEquals(candidateClose, result[0])
        assertEquals(candidateMedium, result[1])
    }

    @Test
    fun execute_limitsResultsToMaxPoiGeofences() = runBlocking {
        val candidates = (0 until 120).map { i ->
            LocationEntity(
                id = "id-$i",
                alias = "Alias $i",
                latitude = 40.0 + (i * 0.001),
                longitude = -3.0
            )
        }
        whenever(locationDao.getLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(candidates)

        distanceCalculator = { _, _, destLat, _ ->
            val index = Math.round((destLat - 40.0) / 0.001).toInt()
            index * 10f
        }

        val result = useCase.execute(40.0, -3.0, 10.0f)

        assertEquals(SpatialSearchUseCase.MAX_POI_GEOFENCES, result.size)
        assertEquals(99, result.size)
        assertEquals("id-0", result.first().id)
        assertEquals("id-98", result.last().id)
    }

    @Test
    fun execute_whenAllCandidatesOutsideRadius_returnsEmptyList() = runBlocking {
        val candidates = listOf(
            LocationEntity("1", "Too Far", 40.5, -3.5)
        )
        whenever(locationDao.getLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(candidates)

        distanceCalculator = { _, _, _, _ -> 20000f }

        val result = useCase.execute(40.0, -3.0, 5.0f)

        assertTrue(result.isEmpty())
    }
}
