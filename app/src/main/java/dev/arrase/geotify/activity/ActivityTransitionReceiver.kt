package dev.arrase.geotify.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dev.arrase.geotify.geofence.GeofenceRecalculationWorker

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive triggered with intent: $intent")
        if (intent.action != ACTION_ACTIVITY_TRANSITION) {
            Log.d(TAG, "Unknown action: ${intent.action}")
            return
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            var shouldRecalculate = false

            for (event in result.transitionEvents) {
                Log.d(TAG, "Activity Transition: type=${event.activityType}, transition=${event.transitionType}")
                val isEnteringStillOrWalking = (event.activityType == DetectedActivity.STILL || event.activityType == DetectedActivity.WALKING) &&
                        event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
                val isExitingVehicle = event.activityType == DetectedActivity.IN_VEHICLE &&
                        event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT

                if (isEnteringStillOrWalking || isExitingVehicle) {
                    Log.i(TAG, "Destination-arrival transition detected (Activity: ${event.activityType}, Transition: ${event.transitionType})")
                    shouldRecalculate = true
                    break
                }
            }

            if (shouldRecalculate) {
                Log.i(TAG, "Triggering geofence recalculation from activity transition...")
                val workRequest = OneTimeWorkRequestBuilder<GeofenceRecalculationWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork("geofence_recalculation", ExistingWorkPolicy.REPLACE, workRequest)
            }
        }
    }

    companion object {
        const val ACTION_ACTIVITY_TRANSITION = "dev.arrase.geotify.ACTION_ACTIVITY_TRANSITION"
        private const val TAG = "ActivityTransitionRcvr"
    }
}
