package dev.arrase.geotify.geofence

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.arrase.geotify.data.SettingsManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules [GeofenceRecalculationWorker] runs.
 *
 * The two paths use separate unique work names so they cannot cancel one another, and the policy
 * differs by intent: a recalculation replaces the whole GMS geofence set, so a debounced run that is
 * already pending must be *replaced* to collapse a burst of edits into a single run starting after
 * the last one, while an expedited run must be *kept* so that a fix already in progress is never
 * interrupted midway.
 */
@Singleton
class GeofenceOrchestrator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {

    /**
     * Enqueues an expedited recalculation as soon as the platform allows.
     * Safe to call from a non-suspending context such as `BroadcastReceiver.onReceive`.
     *
     * Under Android 12+ expedited work may run immediately in the background. If the quota is
     * exhausted the request degrades to a normal one; since the work is a local database query
     * plus a GMS call it does not need a foreground-service notification.
     */
    fun triggerExpeditedRecalculation() {
        val request = OneTimeWorkRequestBuilder<GeofenceRecalculationWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        enqueue(EXPEDITED_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Enqueues a recalculation after the user-configured debounce delay. Successive calls replace
     * the pending one, so a burst of edits results in a single run once the user stops changing
     * things.
     */
    suspend fun triggerRecalculation() {
        val debounceSecs = settingsManager.recalculationDebounceSecs.first().toLong()
        val request = OneTimeWorkRequestBuilder<GeofenceRecalculationWorker>()
            .setInitialDelay(debounceSecs, TimeUnit.SECONDS)
            .build()
        enqueue(DEBOUNCED_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun enqueue(
        uniqueWorkName: String,
        policy: ExistingWorkPolicy,
        request: OneTimeWorkRequest
    ) {
        try {
            WorkManager.getInstance(context).enqueueUniqueWork(uniqueWorkName, policy, request)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "WorkManager not initialized; skipping recalculation.", e)
        }
    }

    private companion object {
        const val TAG = "GeotifyOrchestrator"
        const val EXPEDITED_WORK_NAME = "geofence_recalculation_expedited"
        const val DEBOUNCED_WORK_NAME = "geofence_recalculation_debounced"
    }
}
