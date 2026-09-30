package dev.arrase.geotify.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Re-registers the geofence window after the device boots or the app is updated, since Play
 * services drops every geofence in both cases.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    fun interface BootEntryPoint {
        fun geofenceOrchestrator(): GeofenceOrchestrator
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RELEVANT_ACTIONS) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted. Skipping geofence re-registration.")
            return
        }

        Log.i(TAG, "Triggering geofence re-registration after ${intent.action}")
        EntryPointAccessors.fromApplication(context, BootEntryPoint::class.java)
            .geofenceOrchestrator()
            .triggerExpeditedRecalculation()
    }

    private companion object {
        const val TAG = "GeotifyBootReceiver"
        val RELEVANT_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
