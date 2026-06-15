package dev.arrase.geotify

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dev.arrase.geotify.appfunction.GeotifyAppFunctions
import dev.arrase.geotify.data.GeotifyDatabase
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.geofence.AndroidGeofenceManager
import dev.arrase.geotify.geofence.GeofenceManager
import dev.arrase.geotify.location.DefaultLocationProvider
import dev.arrase.geotify.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeotifyApplication : Application(), AppFunctionConfiguration.Provider {

    private val database by lazy { GeotifyDatabase.getInstance(this) }

    private val geofenceManager: GeofenceManager by lazy { AndroidGeofenceManager(this) }

    val repository by lazy {
        GeotifyRepository(database.locationDao(), database.reminderDao(), geofenceManager)
    }

    val locationProvider by lazy { DefaultLocationProvider(this) }

    val settingsManager by lazy { SettingsManager(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.reRegisterAllActiveGeofences()
        }
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeotifyAppFunctions::class.java) {
                GeotifyAppFunctions(repository, locationProvider)
            }
            .build()
}
