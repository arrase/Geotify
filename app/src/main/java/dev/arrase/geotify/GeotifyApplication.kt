package dev.arrase.geotify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.arrase.geotify.notification.NotificationHelper

@HiltAndroidApp
class GeotifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
