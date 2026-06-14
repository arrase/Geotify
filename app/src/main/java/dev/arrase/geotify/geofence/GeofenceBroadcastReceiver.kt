package dev.arrase.geotify.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import dev.arrase.geotify.notification.NotificationHelper
import dev.arrase.geotify.util.geotifyRepository
import dev.arrase.geotify.util.goAsyncCoroutine

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        goAsyncCoroutine {
            try {
                val repository = context.geotifyRepository

                for (geofence in triggeringGeofences) {
                    val locationId = geofence.requestId
                    val location = repository.findLocationById(locationId) ?: continue
                    val transitionType = geofencingEvent.geofenceTransition

                    val activeReminders = repository.getActiveRemindersForLocation(locationId)
                        .filter { it.transitionType == transitionType }

                    for (reminder in activeReminders) {
                        repository.deactivateReminder(reminder.id)

                        NotificationHelper.showGeofenceNotification(
                            context,
                            reminder.id.hashCode(),
                            reminder.id,
                            location.alias,
                            reminder.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence event", e)
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceBroadcastRcvr"
    }
}
