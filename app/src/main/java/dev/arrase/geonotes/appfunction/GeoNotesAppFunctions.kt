package dev.arrase.geonotes.appfunction

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.arrase.geonotes.data.GeoNotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Serializable result returned after initiating a location save. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SaveLocationResult(
    /** The alias assigned to the location being saved. */
    val alias: String,
    /** Human-readable status describing the save operation progress. */
    val status: String
)

/** Serializable result returned after creating a geofence reminder. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CreateReminderResult(
    /** Unique identifier of the newly created reminder. */
    val reminderId: String,
    /** The alias of the target location for this reminder. */
    val targetAlias: String,
    /** The reminder message that will be displayed when triggered. */
    val payloadMessage: String,
    /** Whether the reminder triggers on "arrival" or "departure". */
    val triggerType: String
)

/** Serializable representation of a saved location. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SavedLocation(
    /** The human-readable alias name for this location. */
    val alias: String,
    /** The latitude coordinate of the saved location. */
    val latitude: Double,
    /** The longitude coordinate of the saved location. */
    val longitude: Double
)

/** Serializable result returned after a delete operation. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DeleteResult(
    /** The alias of the deleted location or reminder target. */
    val alias: String,
    /** Whether the deletion was successful. */
    val deleted: Boolean
)

class GeoNotesAppFunctions(
    private val repository: GeoNotesRepository,
    private val applicationContext: Context
) {

    /**
     * Save the device's exact GPS location right now, assigning it the given alias name.
     * Examples: 'mom's house', 'supermarket', 'gym'.
     * The location is obtained in the background via GPS and may take a few seconds to complete.
     *
     * @param alias The alias to assign to the current location (e.g. 'casa', 'trabajo').
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun saveCurrentLocation(
        appFunctionContext: AppFunctionContext,
        alias: String
    ): SaveLocationResult = withContext(Dispatchers.IO) {
        val client = LocationServices.getFusedLocationProviderClient(applicationContext)
        val cancellationSource = CancellationTokenSource()

        try {
            @Suppress("MissingPermission")
            val location = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationSource.token
            ).await()

            if (location != null) {
                repository.saveLocation(alias, location.latitude, location.longitude)
                SaveLocationResult(alias, "Location successfully saved.")
            } else {
                SaveLocationResult(alias, "Failed to obtain GPS fix.")
            }
        } catch (e: Exception) {
            SaveLocationResult(alias, "Error: ${e.message}")
        }
    }

    /**
     * Create a location-based reminder. It triggers when the user crosses the geofence
     * of the location referenced by 'targetAlias'. Set 'triggerOnArrival' to true to trigger
     * when arriving, or false to trigger when leaving.
     *
     * @param targetAlias The exact alias of a previously saved location. Use listLocations to see available aliases.
     * @param payloadMessage The reminder message to display when triggered.
     * @param triggerOnArrival True = remind on arrival (entering the area). False = remind on departure (leaving the area).
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createGeofenceReminder(
        appFunctionContext: AppFunctionContext,
        targetAlias: String,
        payloadMessage: String,
        triggerOnArrival: Boolean
    ): CreateReminderResult = withContext(Dispatchers.IO) {
        val location = repository.findLocationByAlias(targetAlias)
            ?: throwAliasNotFound(targetAlias)

        val transitionType = if (triggerOnArrival) {
            Geofence.GEOFENCE_TRANSITION_ENTER
        } else {
            Geofence.GEOFENCE_TRANSITION_EXIT
        }

        val reminder = repository.createReminder(location, payloadMessage, transitionType)

        CreateReminderResult(
            reminderId = reminder.id,
            targetAlias = targetAlias,
            payloadMessage = payloadMessage,
            triggerType = if (triggerOnArrival) "arrival" else "departure"
        )
    }

    /**
     * List all saved locations with their alias names and coordinates.
     * Use this to check available locations before creating a reminder.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listLocations(
        appFunctionContext: AppFunctionContext
    ): List<SavedLocation> = withContext(Dispatchers.IO) {
        repository.getAllLocations().map { entity ->
            SavedLocation(entity.alias, entity.latitude, entity.longitude)
        }
    }

    /**
     * Delete a saved location by its alias. This also removes all reminders
     * and geofences associated with that location.
     *
     * @param alias The alias of the saved location to delete (e.g. 'casa').
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteLocation(
        appFunctionContext: AppFunctionContext,
        alias: String
    ): DeleteResult = withContext(Dispatchers.IO) {
        repository.findLocationByAlias(alias) ?: throwAliasNotFound(alias)
        repository.deleteLocation(alias)
        DeleteResult(alias, deleted = true)
    }

    /**
     * Cancel and delete a specific active reminder by matching the location alias
     * and reminder message.
     *
     * @param targetAlias The alias of the location associated with the reminder.
     * @param message The reminder message to match and delete.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteReminder(
        appFunctionContext: AppFunctionContext,
        targetAlias: String,
        message: String
    ): DeleteResult = withContext(Dispatchers.IO) {
        val location = repository.findLocationByAlias(targetAlias)
            ?: throwAliasNotFound(targetAlias)

        val activeReminders = repository.getActiveReminders()
            .filter { it.locationId == location.id }

        val matched = activeReminders.firstOrNull {
            it.message.contains(message, ignoreCase = true)
        } ?: throw AppFunctionInvalidArgumentException(
            "No active reminder matching '$message' for '$targetAlias'. " +
                "Active reminders: ${activeReminders.joinToString { "'${it.message}'" }}"
        )

        repository.cancelReminder(matched.id)
        DeleteResult(targetAlias, deleted = true)
    }

    private suspend fun throwAliasNotFound(alias: String): Nothing {
        val aliasList = repository.getAllAliases().joinToString()
        throw AppFunctionInvalidArgumentException(
            "Alias '$alias' not found. Valid locations are: $aliasList. " +
                "Map the user's intent to an exact value from this list and invoke again."
        )
    }
}
