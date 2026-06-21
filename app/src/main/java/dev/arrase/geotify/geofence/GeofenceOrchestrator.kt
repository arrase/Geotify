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

@Singleton
class GeofenceOrchestrator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {

    /**
     * Enqueues an expedited recalculation worker immediately.
     * Safe to call from non-suspend context (e.g. BroadcastReceiver.onReceive).
     *
     * Note on FOREGROUND_SERVICE: Under Android 12+, expedited work can run immediately even
     * when the app is in the background. We use [OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST]
     * to fallback to a regular work request if quota is exhausted. Since this work runs in
     * milliseconds (purely local DB query and geofence updates), we do not need to bind a
     * Foreground Service notification, avoiding a flashing notification to the user.
     */
    fun triggerExpeditedRecalculation() {
        Log.i(TAG, "Enqueuing EXPEDITED geofence recalculation...")
        val request = OneTimeWorkRequestBuilder<GeofenceRecalculationWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        enqueue(request)
    }

    /**
     * Enqueues a debounced recalculation worker.
     * The debounce delay is read from [SettingsManager] and uses [ExistingWorkPolicy.REPLACE]
     * so rapid successive calls collapse into a single execution.
     */
    suspend fun triggerRecalculation() {
        val debounceSecs = settingsManager.recalculationDebounceSecs.first().toLong()
        Log.i(TAG, "Enqueuing debounced geofence recalculation (delay=${debounceSecs}s)...")
        val request = OneTimeWorkRequestBuilder<GeofenceRecalculationWorker>()
            .setInitialDelay(debounceSecs, TimeUnit.SECONDS)
            .build()
        enqueue(request)
    }

    private fun enqueue(request: OneTimeWorkRequest) {
        try {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "WorkManager not initialized (test environment). Skipping enqueue.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue recalculation worker", e)
        }
    }

    companion object {
        private const val TAG = "GeofenceOrchestrator"
        private const val WORK_NAME = "geofence_recalculation"
    }
}
