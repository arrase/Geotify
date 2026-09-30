package dev.arrase.geotify.geofence

import dev.arrase.geotify.data.entity.LocationEntity

/** Registers and clears the geofences monitored by Google Play services. */
interface GeofenceManager {

    /** Removes every geofence previously registered by this app. */
    suspend fun removeAllGeofences()

    /**
     * Replaces the monitored set with a master geofence plus one geofence per entry in [locations],
     * keyed by location and mapped to its GMS transition-type bitmask.
     *
     * @return `true` if GMS accepted the new window, `false` if a required permission is missing.
     */
    suspend fun registerSlidingWindowGeofences(
        locations: Map<LocationEntity, Int>,
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ): Boolean

    /**
     * Registers only the master geofence, used when no POI is currently in range.
     *
     * @return `true` if GMS accepted it, `false` if a required permission is missing.
     */
    suspend fun registerMasterGeofence(
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ): Boolean

    companion object {
        /** Request id of the geofence centred on the user, used to detect leaving the inner radius. */
        const val MASTER_REQUEST_ID = "MASTER_GEOFENCE_TRIGGER"
    }
}
