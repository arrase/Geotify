package dev.arrase.geotify.ui.screen

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arrase.geotify.R
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.location.LocationProvider
import dev.arrase.geotify.ui.BaseViewModel
import dev.arrase.geotify.ui.UiText
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
    private val geofenceOrchestrator: GeofenceOrchestrator,
    private val locationProvider: LocationProvider,
    settingsManager: SettingsManager
) : BaseViewModel() {

    val locations: StateFlow<List<LocationEntity>> = locationRepository.observeLocations()
        .settingFlow(emptyList())

    val activeReminderCounts: StateFlow<Map<String, Int>> =
        reminderRepository.observeActiveReminderCounts()
            .map { counts -> counts.associate { it.locationId to it.count } }
            .settingFlow(emptyMap())

    val mapTheme: StateFlow<ThemeSetting> =
        settingsManager.mapTheme.settingFlow(SettingsDefaults.MAP_THEME)

    fun saveLocation(
        alias: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        notificationResponsivenessMs: Int
    ) = viewModelScope.launch {
        mutate(ERROR_SAVE, onSuccess = geofenceOrchestrator::triggerRecalculation) {
            locationRepository.saveLocation(
                alias = alias,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters,
                notificationResponsivenessMs = notificationResponsivenessMs
            )
        }
    }

    fun updateLocation(location: LocationEntity) = viewModelScope.launch {
        mutate(ERROR_UPDATE, onSuccess = geofenceOrchestrator::triggerRecalculation) {
            locationRepository.updateLocation(location)
        }
    }

    fun deleteLocation(location: LocationEntity) = viewModelScope.launch {
        mutate(
            errorFallback = ERROR_DELETE,
            onSuccess = {
                geofenceOrchestrator.triggerRecalculation()
                messages.send(
                    UiText.StringResource(R.string.location_deleted, location.alias)
                )
            }
        ) {
            if (!locationRepository.deleteLocation(location.alias)) {
                throw StaleLocationException(location.id)
            }
        }
    }

    suspend fun getCurrentLocation() = locationProvider.getCurrentLocation()

    private companion object {
        const val ERROR_SAVE = "Could not save the location"
        const val ERROR_UPDATE = "Could not update the location"
        const val ERROR_DELETE = "Could not delete the location"
    }
}

/** Raised when a delete affected no rows, i.e. the location was already gone. */
private class StaleLocationException(id: String) :
    IllegalStateException("Location $id no longer exists")
