package dev.arrase.geotify.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.notification.NotificationHelper
import dev.arrase.geotify.util.goAsyncCoroutine

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun locationRepository(): LocationRepository
        fun reminderRepository(): ReminderRepository
        fun geofenceOrchestrator(): GeofenceOrchestrator
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered with intent: $intent")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "GeofencingEvent is null in received intent")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error code: ${geofencingEvent.errorCode}")
            return
        }

        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "No triggering geofences found in event")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        Log.d(TAG, "Triggered geofences count: ${triggeringGeofences.size}, transitionType: $transitionType")

        val entryPoint = EntryPointAccessors.fromApplication(
            context, ReceiverEntryPoint::class.java
        )

        val hasMasterExit = triggeringGeofences.any { it.requestId == "MASTER_GEOFENCE_TRIGGER" } &&
                transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
        if (hasMasterExit) {
            Log.i(TAG, "Master geofence exit triggered. Enqueuing expedited recalculation...")
            entryPoint.geofenceOrchestrator().triggerExpeditedRecalculation()
        }

        val poiGeofences = triggeringGeofences.filter { it.requestId != "MASTER_GEOFENCE_TRIGGER" }
        if (poiGeofences.isEmpty()) return

        goAsyncCoroutine {
            try {
                val locationRepo = entryPoint.locationRepository()
                val reminderRepo = entryPoint.reminderRepository()

                for (geofence in poiGeofences) {
                    val locationId = geofence.requestId
                    val location = locationRepo.findLocationById(locationId)
                    if (location == null) {
                        Log.d(TAG, "Location not found in database for geofence ID: $locationId")
                        continue
                    }
                    Log.d(TAG, "Processing geofence for location: ${location.alias} (ID: $locationId)")

                    val activeReminders = reminderRepo.getActiveRemindersForLocation(locationId)
                    Log.d(TAG, "Found ${activeReminders.size} active reminders for location ID: $locationId")
                    
                    val matchingReminders = activeReminders.filter { it.transitionType == transitionType }
                    Log.d(TAG, "Found ${matchingReminders.size} matching reminders for transitionType: $transitionType")

                    for (reminder in matchingReminders) {
                        Log.d(TAG, "Deactivating and showing notification for reminder ID: ${reminder.id}")
                        reminderRepo.deactivateReminder(reminder.id)

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

            // Re-evaluate geofences after deactivating triggered reminders.
            // If no active reminders remain, this will purge all geofences to save battery.
            entryPoint.geofenceOrchestrator().triggerExpeditedRecalculation()
        }
    }

    companion object {
        private const val TAG = "GeofenceBroadcastRcvr"
    }
}
