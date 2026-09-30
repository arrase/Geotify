package dev.arrase.geotify.domain

import android.location.Location
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.entity.LocationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

@OptIn(ExperimentalCoroutinesApi::class)
class SpatialSearchUseCaseTest {

    private val locationRepository: LocationRepository = mock()
    private lateinit var useCase: SpatialSearchUseCase
    private lateinit var mockedLocation: MockedStatic<Location>
    private var distanceCalculator: (Double, Double, Double, Double) -> Float = { _, _, _, _ -> 0f }

    @Before
    fun setUp() {
        useCase = SpatialSearchUseCase(locationRepository, UnconfinedTestDispatcher())
        mockedLocation = mockStatic(Location::class.java)
        mockedLocation.`when`<Any> {
            Location.distanceBetween(any(), any(), any(), any(), any<FloatArray>())
        }.thenAnswer { invocation ->
            val buffer = invocation.getArgument<FloatArray>(4)
            buffer[0] = distanceCalculator(
                invocation.getArgument(0),
                invocation.getArgument(1),
                invocation.getArgument(2),
                invocation.getArgument(3)
            )
            null
        }
    }

    @After
    fun tearDown() {
        mockedLocation.close()
    }

    @Test
    fun `computes the expected bounding box`() = runTest {
        val centerLat = 40.0
        val centerLon = -3.0
        val radiusKm = 10f
        val latDelta = radiusKm * 1000.0 / 111_320.0
        val lonDelta = radiusKm * 1000.0 / (111_320.0 * cos(Math.toRadians(centerLat)))

        givenNoCandidates()

        useCase(centerLat, centerLon, radiusKm)

        val box = captureBoundingBox()
        assertEquals(centerLat - latDelta, box.minLat, 0.0001)
        assertEquals(centerLat + latDelta, box.maxLat, 0.0001)
        assertEquals(centerLon - lonDelta, box.minLon, 0.0001)
        assertEquals(centerLon + lonDelta, box.maxLon, 0.0001)
    }

    @Test
    fun `spans all longitudes at the poles`() = runTest {
        givenNoCandidates()

        useCase(centerLat = 90.0, centerLon = 0.0, radiusKm = 5f)

        val box = captureBoundingBox()
        assertEquals(-360.0, box.minLon, 0.0001)
        assertEquals(360.0, box.maxLon, 0.0001)
    }

    @Test
    fun `produces a window that crosses the antimeridian near longitude 180`() = runTest {
        givenNoCandidates()

        // 500 km at the equator is ~4.5 degrees of longitude, enough to pass 180 from 179.
        useCase(centerLat = 0.0, centerLon = 179.0, radiusKm = 500f)

        val box = captureBoundingBox()
        assertTrue("expected maxLon > 180 but was ${box.maxLon}", box.maxLon > 180.0)
        assertTrue(box.minLon > 0.0)
    }

    @Test
    fun `rejects an out-of-range latitude`() = runTest {
        assertRejects { useCase(centerLat = 91.0, centerLon = 0.0, radiusKm = 1f) }
    }

    @Test
    fun `rejects an out-of-range longitude`() = runTest {
        assertRejects { useCase(centerLat = 0.0, centerLon = 181.0, radiusKm = 1f) }
    }

    @Test
    fun `rejects a negative radius`() = runTest {
        assertRejects { useCase(centerLat = 0.0, centerLon = 0.0, radiusKm = -1f) }
    }

    @Test
    fun `filters candidates beyond the radius and sorts by distance`() = runTest {
        val close = location("1", 40.01, -3.01)
        val medium = location("2", 40.02, -3.02)
        val far = location("3", 40.10, -3.10)
        givenCandidates(medium, far, close)
        distanceCalculator = { _, _, destLat, _ ->
            when (destLat) {
                40.01 -> 1_000f
                40.02 -> 3_000f
                else -> 8_000f
            }
        }

        val result = useCase(40.0, -3.0, 5.0f)

        assertEquals(listOf(close, medium), result)
    }

    @Test
    fun `limits results to the GMS geofence budget`() = runTest {
        val candidates = (0 until 120).map { index ->
            location("id-$index", 40.0 + index * 0.001, -3.0)
        }
        givenCandidates(*candidates.toTypedArray())
        distanceCalculator = { _, _, destLat, _ ->
            (Math.round((destLat - 40.0) / 0.001) * 10).toFloat()
        }

        val result = useCase(40.0, -3.0, 10.0f)

        assertEquals(SpatialSearchUseCase.MAX_POI_GEOFENCES, result.size)
        assertEquals("id-0", result.first().id)
        assertEquals("id-98", result.last().id)
    }

    @Test
    fun `returns nothing when every candidate is out of range`() = runTest {
        givenCandidates(location("1", 40.5, -3.5))
        distanceCalculator = { _, _, _, _ -> 20_000f }

        assertTrue(useCase(40.0, -3.0, 5.0f).isEmpty())
    }

    private suspend fun assertRejects(block: suspend () -> Unit) {
        try {
            block()
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().isNotBlank())
        }
    }

    private fun location(id: String, latitude: Double, longitude: Double) =
        LocationEntity(id = id, alias = "Alias $id", latitude = latitude, longitude = longitude)

    private suspend fun givenNoCandidates() {
        whenever(locationRepository.findLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(emptyList())
    }

    private suspend fun givenCandidates(vararg candidates: LocationEntity) {
        whenever(locationRepository.findLocationsInBoundingBox(any(), any(), any(), any()))
            .thenReturn(candidates.toList())
    }

    private suspend fun captureBoundingBox(): BoundingBox {
        val minLat = ArgumentCaptor.forClass(Double::class.java)
        val maxLat = ArgumentCaptor.forClass(Double::class.java)
        val minLon = ArgumentCaptor.forClass(Double::class.java)
        val maxLon = ArgumentCaptor.forClass(Double::class.java)
        verify(locationRepository).findLocationsInBoundingBox(
            minLat.capture(), maxLat.capture(), minLon.capture(), maxLon.capture()
        )
        return BoundingBox(minLat.value, maxLat.value, minLon.value, maxLon.value)
    }

    private data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )
}
