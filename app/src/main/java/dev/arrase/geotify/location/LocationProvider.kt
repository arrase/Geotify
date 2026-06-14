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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
}

class DefaultLocationProvider(private val context: Context) : LocationProvider {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }
        try {
            withTimeout(15_000L) {
                val cancellationSource = CancellationTokenSource()
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
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
