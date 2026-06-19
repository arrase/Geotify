package dev.arrase.geotify.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dev.arrase.geotify.data.entity.LocationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import dev.arrase.geotify.location.LocationProvider

@Singleton
class AndroidGeofenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val locationProvider: LocationProvider
) : GeofenceManager {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = "dev.arrase.geotify.ACTION_RECEIVE_GEOFENCE"
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override suspend fun registerGeofenceForLocation(location: LocationEntity, transitionTypes: Int) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        
        Log.i("GeofenceManager", "registerGeofenceForLocation: alias=${location.alias}, id=${location.id}, transitionTypes=$transitionTypes")
        Log.i("GeofenceManager", "Permissions check: FINE=$fineLocationPermission, BACKGROUND=$backgroundLocationPermission (GRANTED=${PackageManager.PERMISSION_GRANTED})")

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            backgroundLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("GeofenceManager", "Aborting registration: Permissions not granted!")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(location.id)
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setNotificationResponsiveness(location.notificationResponsivenessMs)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        Log.i("GeofenceManager", "Calling addGeofences for location ${location.alias} with radius ${location.radiusMeters}...")
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.i("GeofenceManager", "Successfully registered geofence in GMS for: ${location.alias}")
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Failed to register geofence in GMS for: ${location.alias}", e)
            throw e
        }

        // Force GMS to evaluate the geofence by requesting a high-accuracy location update
        Log.i("GeofenceManager", "Requesting current location to force GMS geofence evaluation...")
        runCatching {
            val loc = locationProvider.getCurrentLocation()
            Log.i("GeofenceManager", "Location update received: $loc")
        }.onFailure { e ->
            Log.w("GeofenceManager", "Failed to force location update", e)
        }
    }

    override suspend fun removeGeofences(requestIds: List<String>) {
        if (requestIds.isEmpty()) return
        geofencingClient.removeGeofences(requestIds).await()
    }

    override suspend fun removeAllGeofences() {
        Log.i("GeofenceManager", "removeAllGeofences: Purging all geofences registered with pending intent...")
        try {
            geofencingClient.removeGeofences(geofencePendingIntent).await()
            Log.i("GeofenceManager", "Successfully removed all geofences")
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Failed to remove all geofences", e)
            throw e
        }
    }

    override suspend fun registerSlidingWindowGeofences(
        locations: List<LocationEntity>,
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        
        Log.i("GeofenceManager", "registerSlidingWindowGeofences: centerLat=$centerLat, centerLon=$centerLon, innerRadiusMeters=$innerRadiusMeters, locationsCount=${locations.size}")
        Log.i("GeofenceManager", "Permissions check: FINE=$fineLocationPermission, BACKGROUND=$backgroundLocationPermission")

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            backgroundLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("GeofenceManager", "Aborting registration: Permissions not granted!")
            return
        }

        val requestBuilder = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

        // 1. Create and add Master Geofence
        val masterGeofence = Geofence.Builder()
            .setRequestId("MASTER_GEOFENCE_TRIGGER")
            .setCircularRegion(centerLat, centerLon, innerRadiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(0)
            .build()
        requestBuilder.addGeofence(masterGeofence)

        // 2. Create and add POIs geofences
        for (location in locations) {
            val geofence = Geofence.Builder()
                .setRequestId(location.id)
                .setCircularRegion(location.latitude, location.longitude, location.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setNotificationResponsiveness(location.notificationResponsivenessMs)
                .build()
            requestBuilder.addGeofence(geofence)
        }

        val request = requestBuilder.build()

        Log.i("GeofenceManager", "Calling addGeofences for sliding window...")
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.i("GeofenceManager", "Successfully registered sliding window geofences in GMS")
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Failed to register sliding window geofences in GMS", e)
            throw e
        }

        // Force GMS to evaluate the geofence by requesting a high-accuracy location update
        Log.i("GeofenceManager", "Requesting current location to force GMS geofence evaluation...")
        runCatching {
            val loc = locationProvider.getCurrentLocation()
            Log.i("GeofenceManager", "Location update received: $loc")
        }.onFailure { e ->
            Log.w("GeofenceManager", "Failed to force location update", e)
        }
    }
}
