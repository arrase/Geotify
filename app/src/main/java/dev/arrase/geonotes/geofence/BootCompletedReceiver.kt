package dev.arrase.geonotes.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.arrase.geonotes.GeoNotesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val repository = (context.applicationContext as GeoNotesApplication).repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.reRegisterAllActiveGeofences()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-register geofences after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
