package dev.arrase.geotify.geofence

import android.Manifest
import android.content.Context
import android.location.Location
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
import kotlinx.coroutines.CancellationException
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.domain.SpatialSearchUseCase
import dev.arrase.geotify.location.LocationProvider
import kotlinx.coroutines.flow.first

/**
 * Recomputes the sliding window of monitored geofences around the user's current position.
 *
 * The new window is registered with GMS before any derived state is persisted, so a failure
 * can never leave the database claiming geofences that do not exist.
 */
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

        if (!hasLocationPermission()) {
            // Not a failure: the app re-enqueues this work on the next launch or data change.
            Log.w(TAG, "Location permission not granted. Skipping sliding window recalculation.")
            return Result.success()
        }

        return try {
            val location = entryPoint.locationProvider()
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                ?: return retryOrGiveUp("Could not obtain current location")

            val settingsManager = entryPoint.settingsManager()
            val outerRadiusKm = settingsManager.outerRadiusN.first()
            val innerRadiusKm = settingsManager.innerRadiusR.first()

            val activeReminders = entryPoint.reminderRepository().getActiveReminders()
            val candidates = entryPoint.spatialSearchUseCase()(
                centerLat = location.latitude,
                centerLon = location.longitude,
                radiusKm = outerRadiusKm
            ).filter { candidate -> activeReminders.any { it.locationId == candidate.id } }

            registerGeofences(
                geofenceManager = entryPoint.geofenceManager(),
                candidates = candidates,
                activeReminders = activeReminders,
                center = location,
                innerRadiusMeters = innerRadiusKm * METERS_PER_KM
            )?.let {
                // Only persist derived state once GMS has actually accepted the new window,
                // otherwise the UI would claim geofences that do not exist.
                entryPoint.reminderRepository().updateInRangeStatus(candidates.map { it.id })
                settingsManager.setLastRecalcLocation(location.latitude, location.longitude)
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute geofence recalculation", e)
            retryOrGiveUp("Recalculation failed", e)
        }
    }

    /**
     * Installs the new geofence window.
     *
     * @return `true` when GMS holds the new window, `false` when a required permission is
     * missing, or `null` when there was nothing to register because no reminders are active.
     */
    private suspend fun registerGeofences(
        geofenceManager: GeofenceManager,
        candidates: List<LocationEntity>,
        activeReminders: List<ReminderEntity>,
        center: Location,
        innerRadiusMeters: Float
    ): Boolean? {
        // A failure here usually means nothing was registered before, so carrying on is correct.
        runCatching { geofenceManager.removeAllGeofences() }
            .onFailure { Log.w(TAG, "Could not clear previous geofences", it) }

        if (activeReminders.isEmpty()) {
            Log.i(TAG, "No active reminders. All geofences cleared to save battery.")
            return null
        }

        return if (candidates.isNotEmpty()) {
            val registered = geofenceManager.registerSlidingWindowGeofences(
                locations = candidates.toTransitionMasks(activeReminders),
                centerLat = center.latitude,
                centerLon = center.longitude,
                innerRadiusMeters = innerRadiusMeters
            )
            if (registered) {
                Log.i(TAG, "Registered ${candidates.size} POI geofences + master geofence")
            }
            registered
        } else {
            val registered = geofenceManager.registerMasterGeofence(
                centerLat = center.latitude,
                centerLon = center.longitude,
                innerRadiusMeters = innerRadiusMeters
            )
            if (registered) {
                Log.i(TAG, "No POIs in range. Registered master geofence only.")
            }
            registered
        }
    }

    /**
     * Retries with WorkManager's backoff, but gives up permanently after [MAX_ATTEMPTS] so a device
     * that can never obtain a fix does not accumulate an unbounded retry chain.
     */
    private fun retryOrGiveUp(reason: String, cause: Throwable? = null): Result =
        if (runAttemptCount >= MAX_ATTEMPTS) {
            Log.e(TAG, "$reason. Giving up after $MAX_ATTEMPTS attempts.", cause)
            Result.failure()
        } else {
            Log.w(TAG, "$reason. Retrying (attempt ${runAttemptCount + 1}).", cause)
            Result.retry()
        }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val TAG = "GeofenceRecalcWorker"
        const val METERS_PER_KM = 1000f
        const val MAX_ATTEMPTS = 3

        /**
         * Collapses the reminders attached to each candidate into the single transition bitmask
         * that its geofence must monitor (GMS expects the union of the desired transitions).
         * Candidates are pre-filtered to locations that have at least one active reminder, so
         * every mask resolves to at least one transition.
         */
        fun List<LocationEntity>.toTransitionMasks(
            reminders: List<ReminderEntity>
        ): Map<LocationEntity, Int> {
            val masksByLocation = HashMap<String, Int>()
            reminders.forEach { reminder ->
                masksByLocation.merge(reminder.locationId, reminder.transitionType, Int::or)
            }
            return associateWith { location -> masksByLocation.getValue(location.id) }
        }
    }
}
