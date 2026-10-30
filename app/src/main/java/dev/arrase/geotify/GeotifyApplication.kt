package dev.arrase.geotify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.arrase.geotify.notification.NotificationHelper
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class GeotifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        val osmdroidDir = File(cacheDir, "osmdroid")
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = osmdroidDir
            osmdroidTileCache = osmdroidDir
        }
    }
}
