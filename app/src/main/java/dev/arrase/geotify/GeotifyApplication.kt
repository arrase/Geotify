package dev.arrase.geotify

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dagger.hilt.android.HiltAndroidApp
import dev.arrase.geotify.appfunction.GeotifyAppFunctions
import dev.arrase.geotify.activity.ActivityRecognitionHelper
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.di.IoDispatcher
import dev.arrase.geotify.location.LocationProvider
import dev.arrase.geotify.notification.NotificationHelper
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GeotifyApplication : Application(), AppFunctionConfiguration.Provider {

    @Inject
    lateinit var repository: GeotifyRepository

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var activityRecognitionHelper: ActivityRecognitionHelper

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        Log.i("GeotifyApp", "GeotifyApplication.onCreate() - Initializing notification channels...")
        NotificationHelper.createNotificationChannels(this)
        Log.i("GeotifyApp", "GeotifyApplication.onCreate() - Registering activity recognition transitions...")
        activityRecognitionHelper.registerTransitions()
        Log.i("GeotifyApp", "GeotifyApplication.onCreate() - Launching active geofence registration...")
        CoroutineScope(ioDispatcher).launch {
            try {
                repository.reRegisterAllActiveGeofences()
                Log.i("GeotifyApp", "GeotifyApplication.onCreate() - reRegisterAllActiveGeofences completed successfully.")
            } catch (e: Exception) {
                Log.e("GeotifyApp", "GeotifyApplication.onCreate() - Failed to reRegisterAllActiveGeofences", e)
            }
        }
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeotifyAppFunctions::class.java) {
                GeotifyAppFunctions(repository, locationProvider)
            }
            .build()
}
