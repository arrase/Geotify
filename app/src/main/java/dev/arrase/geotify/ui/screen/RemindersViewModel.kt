package dev.arrase.geotify.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.location.LocationProvider
import dev.arrase.geotify.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
    private val geofenceOrchestrator: GeofenceOrchestrator,
    private val locationProvider: LocationProvider,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _snackbarMessage = MutableSharedFlow<UiText>()
    val snackbarMessage: SharedFlow<UiText> = _snackbarMessage.asSharedFlow()

    val locations: StateFlow<List<LocationEntity>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = reminderRepository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeReminderCounts: StateFlow<Map<String, Int>> = reminderRepository.observeActiveReminderCounts()
        .map { list -> list.associate { it.locationId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    val lastRecalcLat: StateFlow<Double?> = settingsManager.lastRecalcLat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val lastRecalcLng: StateFlow<Double?> = settingsManager.lastRecalcLng
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val innerRadiusR: StateFlow<Float> = settingsManager.innerRadiusR
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 4.0f)

    suspend fun getCurrentLocation(): android.location.Location? {
        return locationProvider.getCurrentLocation()
    }

    fun createReminder(locationId: String, message: String, transitionType: Int) {
        viewModelScope.launch {
            try {
                val location = locationRepository.findLocationById(locationId) ?: return@launch
                reminderRepository.createReminder(location, message, transitionType)
                geofenceOrchestrator.triggerRecalculation()
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString("Error: ${e.message}"))
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity, oldLocationId: String) {
        viewModelScope.launch {
            try {
                reminderRepository.updateReminder(reminder)
                geofenceOrchestrator.triggerRecalculation()
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString("Error: ${e.message}"))
            }
        }
    }

    fun cancelReminder(reminderId: String) {
        viewModelScope.launch {
            try {
                reminderRepository.cancelReminder(reminderId)
                geofenceOrchestrator.triggerRecalculation()
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString("Error: ${e.message}"))
            }
        }
    }
}
