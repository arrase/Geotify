package dev.arrase.geotify

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import dev.arrase.geotify.appfunction.GeotifyAppFunctions
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.notification.NotificationHelper
import javax.inject.Inject

@HiltAndroidApp
class GeotifyApplication : Application(), AppFunctionConfiguration.Provider {

    @Inject
    lateinit var geofenceOrchestrator: GeofenceOrchestrator

    @Inject
    lateinit var geotifyAppFunctions: GeotifyAppFunctions

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing notification channels...")
        NotificationHelper.createNotificationChannels(this)
        Log.i(TAG, "Enqueuing geofence recalculation...")
        geofenceOrchestrator.triggerExpeditedRecalculation()
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeotifyAppFunctions::class.java) {
                geotifyAppFunctions
            }
            .build()

    companion object {
        private const val TAG = "GeotifyApp"
    }
}
