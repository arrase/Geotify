package dev.arrase.geotify.ui.screen

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arrase.geotify.R
import dev.arrase.geotify.data.LatLng
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.location.LocationProvider
import dev.arrase.geotify.ui.BaseViewModel
import dev.arrase.geotify.ui.UiText
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
    private val geofenceOrchestrator: GeofenceOrchestrator,
    private val locationProvider: LocationProvider,
    settingsManager: SettingsManager
) : BaseViewModel() {

    val locations: StateFlow<List<LocationEntity>> = locationRepository.observeLocations()
        .settingFlow(emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = reminderRepository.observeReminders()
        .settingFlow(emptyList())

    val mapTheme: StateFlow<ThemeSetting> =
        settingsManager.mapTheme.settingFlow(SettingsDefaults.MAP_THEME)

    val lastRecalcLocation: StateFlow<LatLng?> = settingsManager.lastRecalcLocation
        .settingFlow(null)

    val innerRadiusR: StateFlow<Float> =
        settingsManager.innerRadiusR.settingFlow(SettingsDefaults.INNER_RADIUS_R)

    val outerRadiusN: StateFlow<Float> =
        settingsManager.outerRadiusN.settingFlow(SettingsDefaults.OUTER_RADIUS_N)

    fun createReminder(locationId: String, message: String, transitionType: Int) =
        viewModelScope.launch {
            val location = locationRepository.findLocationById(locationId)
            if (location == null) {
                messages.send(UiText.StringResource(R.string.err_location_missing))
                return@launch
            }
            mutate(ERROR_CREATE, onSuccess = geofenceOrchestrator::triggerRecalculation) {
                reminderRepository.createReminder(location, message, transitionType)
            }
        }

    fun updateReminder(reminder: ReminderEntity) = viewModelScope.launch {
        mutate(ERROR_UPDATE, onSuccess = geofenceOrchestrator::triggerRecalculation) {
            reminderRepository.updateReminder(reminder)
        }
    }

    /**
     * Removes a reminder. [cancelled] distinguishes stopping an active reminder (it stays in the
     * completed list) from deleting a completed one, so the confirmation matches the action.
     */
    fun cancelReminder(reminderId: String, cancelled: Boolean = true) = viewModelScope.launch {
        mutate(
            errorFallback = ERROR_CANCEL,
            onSuccess = {
                geofenceOrchestrator.triggerRecalculation()
                messages.send(
                    UiText.StringResource(
                        if (cancelled) R.string.reminder_cancelled else R.string.reminder_deleted
                    )
                )
            }
        ) {
            if (!reminderRepository.cancelReminder(reminderId)) {
                throw StaleReminderException(reminderId)
            }
        }
    }

    suspend fun getCurrentLocation() = locationProvider.getCurrentLocation()

    private companion object {
        const val ERROR_CREATE = "Could not create the reminder"
        const val ERROR_UPDATE = "Could not update the reminder"
        const val ERROR_CANCEL = "Could not cancel the reminder"
    }
}

/** Raised when a delete affected no rows, i.e. the reminder was already gone. */
private class StaleReminderException(id: String) :
    IllegalStateException("Reminder $id no longer exists")
