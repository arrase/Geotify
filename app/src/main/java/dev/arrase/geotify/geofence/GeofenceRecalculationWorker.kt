package dev.arrase.geotify.geofence

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.GeotifyRepository
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
        fun repository(): GeotifyRepository
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
        val repository = entryPoint.repository()
        val settingsManager = entryPoint.settingsManager()
        val spatialSearchUseCase = entryPoint.spatialSearchUseCase()
        val locationProvider = entryPoint.locationProvider()
        val geofenceManager = entryPoint.geofenceManager()

        try {
            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                Log.w(TAG, "Could not obtain current location. Cannot recalculate sliding window.")
                return Result.failure()
            }

            val centerLat = location.latitude
            val centerLon = location.longitude

            val outerRadiusN = settingsManager.outerRadiusN.first()
            val innerRadiusR = settingsManager.innerRadiusR.first()

            Log.i(TAG, "Current Location: ($centerLat, $centerLon). Radii: N=$outerRadiusN km, r=$innerRadiusR km")

            val spatialCandidates = spatialSearchUseCase.execute(centerLat, centerLon, outerRadiusN)

            val activeReminders = repository.getActiveReminders()
            val activeLocationIds = activeReminders.map { it.locationId }.toSet()

            val activeCandidates = spatialCandidates.filter { candidate ->
                activeLocationIds.contains(candidate.id)
            }

            Log.i(TAG, "Found ${spatialCandidates.size} spatial candidate locations. Filtered to ${activeCandidates.size} with active reminders.")

            Log.i(TAG, "Purging previous geofences...")
            try {
                geofenceManager.removeAllGeofences()
            } catch (e: Exception) {
                Log.w(TAG, "Error removing old geofences (might be none registered)", e)
            }

            if (activeCandidates.isNotEmpty() || activeLocationIds.isNotEmpty()) {
                val innerRadiusMeters = innerRadiusR * 1000f
                geofenceManager.registerSlidingWindowGeofences(
                    locations = activeCandidates,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    innerRadiusMeters = innerRadiusMeters
                )
                Log.i(TAG, "Successfully registered ${activeCandidates.size} POIs + Master Geofence")
            } else {
                Log.i(TAG, "No active reminders found in database, no geofences registered.")
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
