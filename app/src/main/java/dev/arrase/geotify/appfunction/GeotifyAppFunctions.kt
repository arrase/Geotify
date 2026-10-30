package dev.arrase.geotify.appfunction

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.google.android.gms.location.Geofence
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.arrase.geotify.data.DuplicateAliasException
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.entity.triggerTypeString
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.location.LocationProvider

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

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "GeotifyAppFunctionService",
    appFunctionXmlFileName = "geotify_app_function_service"
)
abstract class GeotifyAppFunctions : AppFunctionService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface ServiceEntryPoint {
        fun locationRepository(): LocationRepository
        fun reminderRepository(): ReminderRepository
        fun geofenceOrchestrator(): GeofenceOrchestrator
        fun locationProvider(): LocationProvider
    }

    internal lateinit var locationRepository: LocationRepository
    internal lateinit var reminderRepository: ReminderRepository
    internal lateinit var geofenceOrchestrator: GeofenceOrchestrator
    internal lateinit var locationProvider: LocationProvider

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ServiceEntryPoint::class.java
        )
        locationRepository = entryPoint.locationRepository()
        reminderRepository = entryPoint.reminderRepository()
        geofenceOrchestrator = entryPoint.geofenceOrchestrator()
        locationProvider = entryPoint.locationProvider()
    }

    /**
     * Saves the device's current GPS location under a unique alias name.
     *
     * @param alias Unique human-readable alias for the current location (e.g., "home", "gym", "supermarket").
     * @return Result containing the assigned alias and save operation status.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun saveCurrentLocation(
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
                geofenceOrchestrator.triggerRecalculation()
                SaveLocationResult(alias, "Location successfully saved.")
            } else {
                SaveLocationResult(alias, "Failed to obtain GPS fix.")
            }
        } catch (e: DuplicateAliasException) {
            SaveLocationResult(
                alias,
                "Alias already in use. Choose a unique name or delete the existing one first."
            )
        } catch (e: Exception) {
            SaveLocationResult(alias, "Error: ${e.message}")
        }
    }

    /**
     * Creates a location-based geofence reminder for a saved location alias that triggers on arrival or departure.
     *
     * @param targetAlias Exact alias of an existing saved location.
     * @param payloadMessage Non-blank reminder message displayed when the geofence triggers.
     * @param triggerOnArrival True to trigger when arriving at the location; false to trigger when departing.
     * @return Details of the newly created geofence reminder.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun createGeofenceReminder(
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
        geofenceOrchestrator.triggerRecalculation()
        return CreateReminderResult(
            reminderId = reminder.id,
            targetAlias = targetAlias,
            payloadMessage = payloadMessage,
            triggerType = reminder.triggerTypeString
        )
    }

    /**
     * Lists all saved locations with their aliases and GPS coordinates.
     *
     * @return List of all saved locations, or an empty list if none exist.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun listLocations(): List<SavedLocation> {
        return locationRepository.getAllLocations().map { entity ->
            SavedLocation(entity.alias, entity.latitude, entity.longitude)
        }
    }

    /**
     * Deletes a saved location by its alias along with all reminders and geofences associated with it.
     *
     * @param alias Exact alias of the saved location to delete.
     * @return Deletion status for the specified location alias.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun deleteLocation(
        alias: String
    ): DeleteResult {
        locationRepository.findLocationByAlias(alias) ?: throwAliasNotFound(alias)
        val deleted = locationRepository.deleteLocation(alias)
        geofenceOrchestrator.triggerRecalculation()
        return DeleteResult(alias, deleted = deleted)
    }

    /**
     * Cancels and deletes an active reminder matching a saved location alias and optional message substring.
     *
     * @param targetAlias Exact alias of the saved location associated with the reminder.
     * @param message Case-insensitive substring of the reminder message to match. If null or blank and only one active reminder exists for the location, deletes that reminder.
     * @return Deletion status for the matched reminder.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun deleteReminder(
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

        val deleted = reminderRepository.cancelReminder(matched.id)
        geofenceOrchestrator.triggerRecalculation()
        return DeleteResult(targetAlias, deleted = deleted)
    }

    /**
     * Lists all active geofence reminders across all saved locations.
     *
     * @return List of active reminders with their IDs, target aliases, messages, and trigger types.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun listActiveReminders(): List<SavedReminder> {
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
