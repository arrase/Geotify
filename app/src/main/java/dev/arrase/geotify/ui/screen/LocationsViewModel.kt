package dev.arrase.geotify.ui.screen

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
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
class LocationsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
    private val geofenceOrchestrator: GeofenceOrchestrator,
    private val locationProvider: LocationProvider,
    settingsManager: SettingsManager
) : ViewModel() {

    private val _snackbarMessage = MutableSharedFlow<UiText>()
    val snackbarMessage: SharedFlow<UiText> = _snackbarMessage.asSharedFlow()

    val locations: StateFlow<List<LocationEntity>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeReminderCounts: StateFlow<Map<String, Int>> = reminderRepository.observeActiveReminderCounts()
        .map { list -> list.associate { it.locationId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsDefaults.MAP_THEME)

    fun saveLocation(alias: String, latitude: Double, longitude: Double, radiusMeters: Float, notificationResponsivenessMs: Int) {
        viewModelScope.launch {
            try {
                locationRepository.saveLocation(alias, latitude, longitude, radiusMeters, notificationResponsivenessMs)
                geofenceOrchestrator.triggerRecalculation()
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString(e.localizedMessage ?: "Unknown error saving location"))
            }
        }
    }

    fun updateLocation(location: LocationEntity) {
        viewModelScope.launch {
            try {
                locationRepository.updateLocation(location)
                geofenceOrchestrator.triggerRecalculation()
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString(e.localizedMessage ?: "Unknown error updating location"))
            }
        }
    }

    fun deleteLocation(alias: String) {
        viewModelScope.launch {
            try {
                locationRepository.deleteLocation(alias)
                geofenceOrchestrator.triggerRecalculation()
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString(e.localizedMessage ?: "Unknown error deleting location"))
            }
        }
    }

    suspend fun getCurrentLocation(): Location? = locationProvider.getCurrentLocation()
}
