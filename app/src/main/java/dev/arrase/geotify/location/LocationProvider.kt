package dev.arrase.geotify.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

interface LocationProvider {
    suspend fun getCurrentLocation(priority: Int = Priority.PRIORITY_HIGH_ACCURACY): Location?
}

@Singleton
class DefaultLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LocationProvider {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(priority: Int): Location? = withContext(ioDispatcher) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        // Check cache (last known location) first to save battery
        try {
            val lastLocation = client.lastLocation.await()
            val cacheTimeoutSecs = settingsManager.locationCacheTimeoutSecs.first()
            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) < (cacheTimeoutSecs * 1000L)) {
                Log.d(TAG, "Using fresh cached location: $lastLocation")
                return@withContext lastLocation
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain cached location", e)
        }

        try {
            withTimeout(15_000L) {
                val cancellationSource = CancellationTokenSource()
                client.getCurrentLocation(
                    priority,
                    cancellationSource.token
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain current location", e)
            null
        }
    }

    companion object {
        private const val TAG = "DefaultLocationProvider"
    }
}
