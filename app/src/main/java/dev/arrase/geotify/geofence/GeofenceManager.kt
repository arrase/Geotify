package dev.arrase.geotify.geofence

import dev.arrase.geotify.data.entity.LocationEntity

interface GeofenceManager {
    suspend fun registerGeofenceForLocation(location: LocationEntity, transitionTypes: Int)
    suspend fun removeGeofences(requestIds: List<String>)
    suspend fun removeAllGeofences()
    suspend fun registerSlidingWindowGeofences(
        locations: Map<LocationEntity, Int>,
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    )
    suspend fun registerMasterGeofence(
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    )
}
