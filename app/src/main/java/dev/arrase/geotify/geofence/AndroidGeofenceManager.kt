package dev.arrase.geotify.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.entity.LocationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidGeofenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) : GeofenceManager {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    /**
     * FLAG_MUTABLE is required here because the GMS GeofencingClient needs to populate
     * the PendingIntent's extras with geofence transition data (triggering geofences,
     * transition type, etc.). FLAG_IMMUTABLE would prevent this and cause silent failures.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_RECEIVE_GEOFENCE
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override suspend fun removeAllGeofences() {
        Log.i(TAG, "Purging all geofences registered with pending intent...")
        geofencingClient.removeGeofences(geofencePendingIntent).await()
        Log.i(TAG, "Successfully removed all geofences")
    }

    override suspend fun registerSlidingWindowGeofences(
        locations: Map<LocationEntity, Int>,
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ): Boolean {
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(buildMasterGeofence(centerLat, centerLon, innerRadiusMeters))
        locations.forEach { (location, transitionTypes) ->
            request.addGeofence(buildPoiGeofence(location, transitionTypes))
        }
        return submit(request.build(), "sliding window (${locations.size} POIs + master)")
    }

    override suspend fun registerMasterGeofence(
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ): Boolean {
        // No initial trigger: the master geofence only listens for exits, and GMS ignores an
        // initial trigger whose transition type is not in the geofence's own transition mask.
        val request = GeofencingRequest.Builder()
            .addGeofence(buildMasterGeofence(centerLat, centerLon, innerRadiusMeters))
            .build()
        return submit(request, "master only")
    }

    private suspend fun buildMasterGeofence(
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ): Geofence = Geofence.Builder()
        .setRequestId(GeofenceManager.MASTER_REQUEST_ID)
        .setCircularRegion(centerLat, centerLon, innerRadiusMeters)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
        .setNotificationResponsiveness(
            settingsManager.masterGeofenceResponsivenessSecs.first() * MILLIS_PER_SECOND
        )
        .build()

    private suspend fun buildPoiGeofence(
        location: LocationEntity,
        transitionTypes: Int
    ): Geofence {
        val defaultResponsiveness =
            settingsManager.poiGeofenceResponsivenessSecs.first() * MILLIS_PER_SECOND
        return Geofence.Builder()
            .setRequestId(location.id)
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            // GMS requires a non-zero responsiveness; fall back to the app-wide default.
            .setNotificationResponsiveness(
                maxOf(defaultResponsiveness, location.notificationResponsivenessMs)
            )
            .build()
    }

    /** @return `true` if GMS now holds this window, `false` if permissions prevented it. */
    @SuppressLint("MissingPermission")
    private suspend fun submit(request: GeofencingRequest, description: String): Boolean {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Skipping registration of $description geofences: permissions not granted")
            return false
        }
        Log.i(TAG, "Registering $description geofences with GMS...")
        geofencingClient.addGeofences(request, geofencePendingIntent).await()
        Log.i(TAG, "Successfully registered $description geofences")
        return true
    }

    /**
     * Geofencing needs a precise-enough fix, so `ACCESS_FINE_LOCATION` is required. Play services
     * only grants it when the user chose precise location, not approximate.
     */
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && backgroundLocation
    }

    private companion object {
        const val TAG = "GeotifyGeofence"
        const val ACTION_RECEIVE_GEOFENCE = "dev.arrase.geotify.ACTION_RECEIVE_GEOFENCE"
        const val MILLIS_PER_SECOND = 1_000
    }
}
