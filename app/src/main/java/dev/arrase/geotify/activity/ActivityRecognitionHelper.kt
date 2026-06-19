package dev.arrase.geotify.activity

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRecognitionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ActivityTransitionReceiver.ACTION_ACTIVITY_TRANSITION
        }
        PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun registerTransitions() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Cannot register transitions: ACTIVITY_RECOGNITION permission not granted.")
            return
        }

        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        Log.i(TAG, "Registering Activity Transition Updates...")
        client.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully registered activity transition updates.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register activity transition updates.", e)
            }
    }

    fun deregisterTransitions() {
        Log.i(TAG, "Deregistering Activity Transition Updates...")
        client.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully removed activity transition updates.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove activity transition updates.", e)
            }
    }

    companion object {
        private const val TAG = "ActivityRecogHelper"
    }
}
