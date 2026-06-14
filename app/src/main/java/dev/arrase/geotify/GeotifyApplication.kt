package dev.arrase.geotify

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dev.arrase.geotify.appfunction.GeotifyAppFunctions
import dev.arrase.geotify.data.GeotifyDatabase
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.geofence.GeofenceManager
import dev.arrase.geotify.notification.NotificationHelper

class GeotifyApplication : Application(), AppFunctionConfiguration.Provider {

    private val database by lazy { GeotifyDatabase.getInstance(this) }

    private val geofenceManager by lazy { GeofenceManager(this) }

    val repository by lazy {
        GeotifyRepository(database.locationDao(), database.reminderDao(), geofenceManager)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeotifyAppFunctions::class.java) {
                GeotifyAppFunctions(repository, this)
            }
            .build()
}
