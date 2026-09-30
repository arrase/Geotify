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
import dev.arrase.geotify.data.entity.notificationId
import dev.arrase.geotify.notification.NotificationHelper
import dev.arrase.geotify.util.goAsyncCoroutine

/** Receives geofence transitions from Google Play services. */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun locationRepository(): LocationRepository
        fun reminderRepository(): ReminderRepository
        fun geofenceOrchestrator(): GeofenceOrchestrator
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.w(TAG, "Received an intent without a GeofencingEvent")
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(context, ReceiverEntryPoint::class.java)
        val orchestrator = entryPoint.geofenceOrchestrator()

        if (event.hasError()) {
            // Errors such as GEOFENCE_TOO_MANY_GEOFENCES or GEOFENCE_NOT_REGISTERED leave the
            // monitored set inconsistent; only a recalculation can recover from them.
            Log.e(TAG, "Geofencing error code: ${event.errorCode}. Scheduling recalculation.")
            orchestrator.triggerExpeditedRecalculation()
            return
        }

        val triggeringGeofences = event.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "GeofencingEvent contained no triggering geofences")
            return
        }

        val transitionType = event.geofenceTransition
        Log.d(TAG, "${triggeringGeofences.size} geofence(s) triggered, transition=$transitionType")

        val masterExited = transitionType == Geofence.GEOFENCE_TRANSITION_EXIT &&
            triggeringGeofences.any { it.requestId == GeofenceManager.MASTER_REQUEST_ID }
        if (masterExited) {
            Log.i(TAG, "Left the recalculation area. Scheduling expedited recalculation.")
            orchestrator.triggerExpeditedRecalculation()
        }

        val poiGeofences = triggeringGeofences
            .filter { it.requestId != GeofenceManager.MASTER_REQUEST_ID }
        if (poiGeofences.isEmpty()) return

        goAsyncCoroutine {
            runCatching {
                processPoiTransitions(
                    context = context,
                    locationRepository = entryPoint.locationRepository(),
                    reminderRepository = entryPoint.reminderRepository(),
                    poiGeofences = poiGeofences,
                    transitionType = transitionType
                )
            }.onFailure { Log.e(TAG, "Failed to process geofence event", it) }

            // Re-evaluate the window now that the triggered reminders are no longer active.
            orchestrator.triggerExpeditedRecalculation()
        }
    }

    private suspend fun processPoiTransitions(
        context: Context,
        locationRepository: LocationRepository,
        reminderRepository: ReminderRepository,
        poiGeofences: List<Geofence>,
        transitionType: Int
    ) {
        poiGeofences.forEach { geofence ->
            val locationId = geofence.requestId
            val location = locationRepository.findLocationById(locationId)
            if (location == null) {
                Log.d(TAG, "No location stored for geofence $locationId; ignoring")
                return@forEach
            }

            val due = reminderRepository.getActiveRemindersForLocation(locationId)
                .filter { it.transitionType == transitionType }

            due.forEach { reminder ->
                NotificationHelper.showGeofenceNotification(
                    context = context,
                    notificationId = reminder.notificationId(),
                    alias = location.alias,
                    message = reminder.message
                )
                reminderRepository.deactivateReminder(reminder.id)
            }
        }
    }

    private companion object {
        const val TAG = "GeotifyGeofenceReceiver"
    }
}
