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

class BootCompletedReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun geofenceOrchestrator(): GeofenceOrchestrator
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted. Skipping geofence re-registration after boot.")
            return
        }

        Log.i(TAG, "Boot completed. Re-registering geofences...")
        val entryPoint = EntryPointAccessors.fromApplication(
            context, BootEntryPoint::class.java
        )
        entryPoint.geofenceOrchestrator().triggerExpeditedRecalculation()
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
