package dev.arrase.geonotes

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dev.arrase.geonotes.appfunction.GeoNotesAppFunctions
import dev.arrase.geonotes.data.GeoNotesDatabase
import dev.arrase.geonotes.data.GeoNotesRepository
import dev.arrase.geonotes.geofence.GeofenceManager
import dev.arrase.geonotes.notification.NotificationHelper

class GeoNotesApplication : Application(), AppFunctionConfiguration.Provider {

    private val database by lazy { GeoNotesDatabase.getInstance(this) }

    private val geofenceManager by lazy { GeofenceManager(this) }

    val repository by lazy {
        GeoNotesRepository(database.locationDao(), database.reminderDao(), geofenceManager)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeoNotesAppFunctions::class.java) {
                GeoNotesAppFunctions(repository, this)
            }
            .build()
}
