package dev.arrase.geotify.domain

import android.location.Location
import dev.arrase.geotify.data.dao.LocationDao
import dev.arrase.geotify.data.entity.LocationEntity
import javax.inject.Inject

class SpatialSearchUseCase @Inject constructor(
    private val locationDao: LocationDao
) {
    suspend fun execute(
        centerLat: Double,
        centerLon: Double,
        radiusN: Float
    ): List<LocationEntity> {
        val radiusInMeters = radiusN * 1000.0
        val latDegreesChange = radiusInMeters / 111320.0
        
        val latRad = Math.toRadians(centerLat)
        val cosLat = Math.cos(latRad)
        val lonDegreesChange = if (cosLat > 0.0) {
            radiusInMeters / (111320.0 * cosLat)
        } else {
            360.0
        }

        val minLat = centerLat - latDegreesChange
        val maxLat = centerLat + latDegreesChange
        val minLon = centerLon - lonDegreesChange
        val maxLon = centerLon + lonDegreesChange

        val candidates = locationDao.getLocationsInBoundingBox(minLat, maxLat, minLon, maxLon)
        
        val resultsWithDistance = mutableListOf<Pair<LocationEntity, Float>>()
        val resultsBuffer = FloatArray(1)

        for (candidate in candidates) {
            Location.distanceBetween(
                centerLat,
                centerLon,
                candidate.latitude,
                candidate.longitude,
                resultsBuffer
            )
            val dist = resultsBuffer[0]
            if (dist <= radiusInMeters) {
                resultsWithDistance.add(Pair(candidate, dist))
            }
        }

        return resultsWithDistance
            .sortedBy { it.second }
            .map { it.first }
            .take(99)
    }
}
