package dev.arrase.geonotes.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import dev.arrase.geonotes.GeoNotesApplication
import dev.arrase.geonotes.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
        val pendingResult = goAsync()
        val repository = (context.applicationContext as GeoNotesApplication).repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
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
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceBroadcastRcvr"
    }
}
