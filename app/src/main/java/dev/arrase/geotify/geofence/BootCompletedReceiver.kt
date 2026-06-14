package dev.arrase.geotify.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.arrase.geotify.util.geotifyRepository
import dev.arrase.geotify.util.goAsyncCoroutine

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        goAsyncCoroutine {
            try {
                context.geotifyRepository.reRegisterAllActiveGeofences()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-register geofences after boot", e)
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
