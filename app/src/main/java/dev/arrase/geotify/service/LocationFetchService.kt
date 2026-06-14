package dev.arrase.geotify.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import dev.arrase.geotify.GeotifyApplication
import dev.arrase.geotify.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationFetchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alias = intent?.getStringExtra("alias") ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NotificationHelper.LOCATION_SERVICE_NOTIFICATION_ID,
            NotificationHelper.buildLocationServiceNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        val app = application as GeotifyApplication
        val locationProvider = app.locationProvider
        val repository = app.repository

        scope.launch {
            try {
                val location = locationProvider.getCurrentLocation()

                if (location != null) {
                    repository.saveLocation(alias, location.latitude, location.longitude)
                } else {
                    Log.w(TAG, "Location provider returned null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch location", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "LocationFetchService"
    }
}
