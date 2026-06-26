package dev.arrase.geotify.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Priority
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.domain.SpatialSearchUseCase
import dev.arrase.geotify.location.LocationProvider
import kotlinx.coroutines.flow.first

class GeofenceRecalculationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecalculationWorkerEntryPoint {
        fun reminderRepository(): ReminderRepository
        fun settingsManager(): SettingsManager
        fun spatialSearchUseCase(): SpatialSearchUseCase
        fun locationProvider(): LocationProvider
        fun geofenceManager(): GeofenceManager
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "GeofenceRecalculationWorker started.")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RecalculationWorkerEntryPoint::class.java
        )
        val reminderRepository = entryPoint.reminderRepository()
        val settingsManager = entryPoint.settingsManager()
        val spatialSearchUseCase = entryPoint.spatialSearchUseCase()
        val locationProvider = entryPoint.locationProvider()
        val geofenceManager = entryPoint.geofenceManager()

        try {
            val fineLocationPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location permission not granted. Cannot recalculate sliding window.")
                return Result.failure()
            }

            val location = locationProvider.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            if (location == null) {
                Log.w(TAG, "Could not obtain current location. Retrying...")
                return Result.retry()
            }

            val centerLat = location.latitude
            val centerLon = location.longitude

            // Save the last recalculation center
            settingsManager.setLastRecalcLocation(centerLat, centerLon)

            val outerRadiusN = settingsManager.outerRadiusN.first()
            val innerRadiusR = settingsManager.innerRadiusR.first()

            Log.i(TAG, "Current Location: ($centerLat, $centerLon). Radii: N=$outerRadiusN km, r=$innerRadiusR km")

            val spatialCandidates = spatialSearchUseCase.execute(centerLat, centerLon, outerRadiusN)

            val activeReminders = reminderRepository.getActiveReminders()
            val activeLocationIds = activeReminders.map { it.locationId }.toSet()

            val activeCandidates = spatialCandidates.filter { candidate ->
                activeLocationIds.contains(candidate.id)
            }

            val activeCandidatesWithTransitions = activeCandidates.associateWith { candidate ->
                activeReminders
                    .filter { it.locationId == candidate.id }
                    .map { it.transitionType }
                    .fold(0) { acc, type -> acc or type }
            }

            Log.i(TAG, "Found ${spatialCandidates.size} spatial candidate locations. Filtered to ${activeCandidates.size} with active reminders.")

            Log.i(TAG, "Purging previous geofences...")
            try {
                geofenceManager.removeAllGeofences()
            } catch (e: Exception) {
                Log.w(TAG, "Error removing old geofences (might be none registered)", e)
            }

            reminderRepository.updateInRangeStatus(activeCandidates.map { it.id })

            if (activeReminders.isEmpty()) {
                Log.i(TAG, "No active reminders. Skipping geofence registration to save battery.")
            } else if (activeCandidatesWithTransitions.isNotEmpty()) {
                val innerRadiusMeters = innerRadiusR * 1000f
                geofenceManager.registerSlidingWindowGeofences(
                    locations = activeCandidatesWithTransitions,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    innerRadiusMeters = innerRadiusMeters
                )
                Log.i(TAG, "Successfully registered ${activeCandidatesWithTransitions.size} POIs + Master Geofence")
            } else {
                val innerRadiusMeters = innerRadiusR * 1000f
                geofenceManager.registerMasterGeofence(
                    centerLat = centerLat,
                    centerLon = centerLon,
                    innerRadiusMeters = innerRadiusMeters
                )
                Log.i(TAG, "No candidates in range but active reminders exist. Registered Master Geofence only.")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute geofence recalculation", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "GeofenceRecalcWorker"
    }
}
