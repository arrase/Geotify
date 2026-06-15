package dev.arrase.geotify.appfunction

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.GeofenceLimitExceededException
import dev.arrase.geotify.data.entity.triggerTypeString
import dev.arrase.geotify.R
import dev.arrase.geotify.location.LocationProvider
import kotlinx.coroutines.Dispatchers
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
    val triggerType: String,
    /** A warning message if the geofence limit is reached. */
    val warning: String? = null
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

/** Serializable representation of an active reminder. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SavedReminder(
    /** Unique identifier of the reminder. */
    val reminderId: String,
    /** The alias of the target location. */
    val targetAlias: String,
    /** The reminder message. */
    val payloadMessage: String,
    /** Whether the reminder triggers on "arrival" or "departure". */
    val triggerType: String
)

class GeotifyAppFunctions(
    private val repository: GeotifyRepository,
    private val locationProvider: LocationProvider
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
        val existing = repository.findLocationByAlias(alias)
        if (existing != null) {
            throw AppFunctionInvalidArgumentException(
                "Location alias '$alias' already exists. Choose a unique name or delete the existing one first."
            )
        }

        try {
            val location = locationProvider.getCurrentLocation()

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

        try {
            val result = repository.createReminder(location, payloadMessage, transitionType)
            val warning = if (result.isLimitWarningTriggered) {
                appFunctionContext.context.getString(R.string.geofence_limit_warning)
            } else null
            CreateReminderResult(
                reminderId = result.reminder.id,
                targetAlias = targetAlias,
                payloadMessage = payloadMessage,
                triggerType = if (triggerOnArrival) "arrival" else "departure",
                warning = warning
            )
        } catch (e: GeofenceLimitExceededException) {
            throw AppFunctionInvalidArgumentException(
                appFunctionContext.context.getString(R.string.geofence_limit_error)
            )
        }
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
     * and optional reminder message.
     *
     * @param targetAlias The alias of the location associated with the reminder.
     * @param message The reminder message to match and delete. If omitted or null,
     *                and only one active reminder exists for the location, it will be deleted.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteReminder(
        appFunctionContext: AppFunctionContext,
        targetAlias: String,
        message: String? = null
    ): DeleteResult = withContext(Dispatchers.IO) {
        val location = repository.findLocationByAlias(targetAlias)
            ?: throwAliasNotFound(targetAlias)

        val activeReminders = repository.getActiveReminders()
            .filter { it.locationId == location.id }

        if (activeReminders.isEmpty()) {
            throw AppFunctionInvalidArgumentException("No active reminders found for '$targetAlias'.")
        }

        val matched = if (message.isNullOrBlank()) {
            if (activeReminders.size == 1) {
                activeReminders.first()
            } else {
                throw AppFunctionInvalidArgumentException(
                    "Multiple active reminders found for '$targetAlias'. Please specify which one to delete by matching its message. " +
                        "Active reminders: ${activeReminders.joinToString { "'${it.message}'" }}"
                )
            }
        } else {
            activeReminders.firstOrNull {
                it.message.contains(message, ignoreCase = true)
            } ?: throw AppFunctionInvalidArgumentException(
                "No active reminder matching '$message' for '$targetAlias'. " +
                    "Active reminders: ${activeReminders.joinToString { "'${it.message}'" }}"
            )
        }

        repository.cancelReminder(matched.id)
        DeleteResult(targetAlias, deleted = true)
    }

    /**
     * List all active reminders across all locations.
     * Use this to check active reminders and their messages.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listActiveReminders(
        appFunctionContext: AppFunctionContext
    ): List<SavedReminder> = withContext(Dispatchers.IO) {
        val reminders = repository.getActiveReminders()
        val locations = repository.getAllLocations().associateBy { it.id }
        reminders.map { entity ->
            val location = locations[entity.locationId]
            val alias = location?.alias ?: "Unknown"
            SavedReminder(
                reminderId = entity.id,
                targetAlias = alias,
                payloadMessage = entity.message,
                triggerType = entity.triggerTypeString
            )
        }
    }

    private suspend fun throwAliasNotFound(alias: String): Nothing {
        val aliases = repository.getAllAliases()
        val message = if (aliases.isEmpty()) {
            "Alias '$alias' not found. No locations are saved yet. Please save a location first using saveCurrentLocation."
        } else {
            "Alias '$alias' not found. Valid locations are: ${aliases.joinToString()}. Map the user's intent to an exact value from this list and invoke again."
        }
        throw AppFunctionInvalidArgumentException(message)
    }
}
