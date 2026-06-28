package dev.arrase.geotify.appfunction

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.entity.triggerTypeString
import dev.arrase.geotify.location.LocationProvider
import javax.inject.Inject

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

@Suppress("UNUSED_PARAMETER")
class GeotifyAppFunctions @Inject constructor(
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
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
    ): SaveLocationResult {
        val existing = locationRepository.findLocationByAlias(alias)
        if (existing != null) {
            throw AppFunctionInvalidArgumentException(
                "Location alias '$alias' already exists. Choose a unique name or delete the existing one first."
            )
        }

        return try {
            val location = locationProvider.getCurrentLocation()

            if (location != null) {
                locationRepository.saveLocation(alias, location.latitude, location.longitude)
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
    ): CreateReminderResult {
        val location = locationRepository.findLocationByAlias(targetAlias)
            ?: throwAliasNotFound(targetAlias)

        val transitionType = if (triggerOnArrival) {
            Geofence.GEOFENCE_TRANSITION_ENTER
        } else {
            Geofence.GEOFENCE_TRANSITION_EXIT
        }

        val reminder = reminderRepository.createReminder(location, payloadMessage, transitionType)
        return CreateReminderResult(
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
    ): List<SavedLocation> {
        return locationRepository.getAllLocations().map { entity ->
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
    ): DeleteResult {
        locationRepository.findLocationByAlias(alias) ?: throwAliasNotFound(alias)
        locationRepository.deleteLocation(alias)
        return DeleteResult(alias, deleted = true)
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
    ): DeleteResult {
        val location = locationRepository.findLocationByAlias(targetAlias)
            ?: throwAliasNotFound(targetAlias)

        val activeReminders = reminderRepository.getActiveReminders()
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

        reminderRepository.cancelReminder(matched.id)
        return DeleteResult(targetAlias, deleted = true)
    }

    /**
     * List all active reminders across all locations.
     * Use this to check active reminders and their messages.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listActiveReminders(
        appFunctionContext: AppFunctionContext
    ): List<SavedReminder> {
        val reminders = reminderRepository.getActiveReminders()
        val locations = locationRepository.getAllLocations().associateBy { it.id }
        return reminders.map { entity ->
            val loc = locations[entity.locationId]
            val alias = loc?.alias ?: "Unknown"
            SavedReminder(
                reminderId = entity.id,
                targetAlias = alias,
                payloadMessage = entity.message,
                triggerType = entity.triggerTypeString
            )
        }
    }

    private suspend fun throwAliasNotFound(alias: String): Nothing {
        val aliases = locationRepository.getAllAliases()
        val message = if (aliases.isEmpty()) {
            "Alias '$alias' not found. No locations are saved yet. Please save a location first using saveCurrentLocation."
        } else {
            "Alias '$alias' not found. Valid locations are: ${aliases.joinToString()}. Map the user's intent to an exact value from this list and invoke again."
        }
        throw AppFunctionInvalidArgumentException(message)
    }
}
