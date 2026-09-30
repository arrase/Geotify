package dev.arrase.geotify.domain

import android.location.Location
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.cos

/**
 * Finds the locations closest to a centre point, closest first, capped at
 * [MAX_POI_GEOFENCES]. Candidates are pre-filtered with a bounding box and then
 * filtered by exact great-circle distance.
 */
class SpatialSearchUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Float
    ): List<LocationEntity> = withContext(ioDispatcher) {
        require(centerLat in -90.0..90.0) { "Latitude must be between -90.0 and 90.0" }
        require(centerLon in -180.0..180.0) { "Longitude must be between -180.0 and 180.0" }
        require(radiusKm >= 0f) { "Radius must be non-negative" }

        val radiusMeters = radiusKm * METERS_PER_KM
        val latDelta = radiusMeters / METERS_PER_DEGREE

        // A degree of longitude shrinks towards the poles, where it reaches zero. Validation
        // above guarantees `centerLat` is within [-90, 90], so `cosLat` is never negative.
        val cosLat = cos(Math.toRadians(centerLat))
        val lonDelta =
            if (cosLat < COS_LAT_EPSILON) 360.0
            else radiusMeters / (METERS_PER_DEGREE * cosLat)

        val candidates = locationRepository.findLocationsInBoundingBox(
            minLat = centerLat - latDelta,
            maxLat = centerLat + latDelta,
            minLon = centerLon - lonDelta,
            maxLon = centerLon + lonDelta
        )

        val distanceBuffer = FloatArray(1)
        candidates
            .map { candidate ->
                Location.distanceBetween(
                    centerLat,
                    centerLon,
                    candidate.latitude,
                    candidate.longitude,
                    distanceBuffer
                )
                candidate to distanceBuffer[0]
            }
            .filter { (_, distance) -> distance <= radiusMeters }
            .sortedBy { (_, distance) -> distance }
            .map { (candidate, _) -> candidate }
            .take(MAX_POI_GEOFENCES)
    }

    companion object {
        private const val METERS_PER_KM = 1000.0

        /** Approximate meters in one degree of latitude. */
        private const val METERS_PER_DEGREE = 111_320.0

        /** Below this |cos(lat)| the longitude span covers the whole globe. */
        private const val COS_LAT_EPSILON = 1e-6

        /** GMS allows a maximum of 100 geofences per app; 1 is reserved for the master geofence. */
        const val MAX_POI_GEOFENCES = 99
    }
}
