package dev.arrase.geotify.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
}

class DefaultLocationProvider(private val context: Context) : LocationProvider {
    override suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cancellationSource = CancellationTokenSource()
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationSource.token
            ).await()
        } catch (e: Exception) {
            null
        }
    }
}
