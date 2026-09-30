package dev.arrase.geotify

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import dagger.hilt.android.HiltAndroidApp
import dev.arrase.geotify.appfunction.GeotifyAppFunctions
import dev.arrase.geotify.notification.NotificationHelper
import javax.inject.Inject

@HiltAndroidApp
class GeotifyApplication : Application(), AppFunctionConfiguration.Provider {

    @Inject
    lateinit var geotifyAppFunctions: GeotifyAppFunctions

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }

    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeotifyAppFunctions::class.java) {
                geotifyAppFunctions
            }
            .build()
}
