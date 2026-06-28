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

import dev.arrase.geotify.data.SettingsManager
import kotlinx.coroutines.flow.first

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
            action = "dev.arrase.geotify.ACTION_RECEIVE_GEOFENCE"
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override suspend fun registerGeofenceForLocation(location: LocationEntity, transitionTypes: Int) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        
        Log.i("GeofenceManager", "registerGeofenceForLocation: alias=${location.alias}, id=${location.id}, transitionTypes=$transitionTypes")
        Log.i("GeofenceManager", "Permissions check: FINE=$fineLocationPermission, BACKGROUND=$backgroundLocationPermission (GRANTED=${PackageManager.PERMISSION_GRANTED})")

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            backgroundLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("GeofenceManager", "Aborting registration: Permissions not granted!")
            return
        }

        val defaultPoiResponsivenessMs = settingsManager.poiGeofenceResponsivenessSecs.first() * 1000
        val geofence = Geofence.Builder()
            .setRequestId(location.id)
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setNotificationResponsiveness(maxOf(defaultPoiResponsivenessMs, location.notificationResponsivenessMs))
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
        locations: Map<LocationEntity, Int>,
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        
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
        val masterResponsivenessMs = settingsManager.masterGeofenceResponsivenessSecs.first() * 1000
        val masterGeofence = Geofence.Builder()
            .setRequestId("MASTER_GEOFENCE_TRIGGER")
            .setCircularRegion(centerLat, centerLon, innerRadiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(masterResponsivenessMs)
            .build()
        requestBuilder.addGeofence(masterGeofence)

        // 2. Create and add POIs geofences
        val defaultPoiResponsivenessMs = settingsManager.poiGeofenceResponsivenessSecs.first() * 1000
        for ((location, transitionTypes) in locations) {
            val geofence = Geofence.Builder()
                .setRequestId(location.id)
                .setCircularRegion(location.latitude, location.longitude, location.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionTypes)
                .setNotificationResponsiveness(maxOf(defaultPoiResponsivenessMs, location.notificationResponsivenessMs)) // batching
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
    }

    override suspend fun registerMasterGeofence(
        centerLat: Double,
        centerLon: Double,
        innerRadiusMeters: Float
    ) {
        val fineLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundLocationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        Log.i("GeofenceManager", "registerMasterGeofence: centerLat=$centerLat, centerLon=$centerLon, innerRadiusMeters=$innerRadiusMeters")

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            backgroundLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("GeofenceManager", "Aborting registration: Permissions not granted!")
            return
        }

        val masterResponsivenessMs = settingsManager.masterGeofenceResponsivenessSecs.first() * 1000
        val masterGeofence = Geofence.Builder()
            .setRequestId("MASTER_GEOFENCE_TRIGGER")
            .setCircularRegion(centerLat, centerLon, innerRadiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(masterResponsivenessMs)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(masterGeofence)
            .build()

        Log.i("GeofenceManager", "Calling addGeofences for master geofence only...")
        try {
            geofencingClient.addGeofences(request, geofencePendingIntent).await()
            Log.i("GeofenceManager", "Successfully registered master geofence only in GMS")
        } catch (e: Exception) {
            Log.e("GeofenceManager", "Failed to register master geofence in GMS", e)
            throw e
        }
    }
}
