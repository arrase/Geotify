package dev.arrase.geotify.ui.screen

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val repository: GeotifyRepository,
    private val locationProvider: LocationProvider,
    settingsManager: SettingsManager
) : ViewModel() {

    val locations: StateFlow<List<LocationEntity>> = repository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeReminderCounts: StateFlow<Map<String, Int>> = repository.observeActiveReminderCounts()
        .map { list -> list.associate { it.locationId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    fun saveLocation(alias: String, latitude: Double, longitude: Double, radiusMeters: Float) {
        viewModelScope.launch {
            repository.saveLocation(alias, latitude, longitude, radiusMeters)
        }
    }

    fun updateLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.updateLocation(location)
        }
    }

    fun deleteLocation(alias: String) {
        viewModelScope.launch { repository.deleteLocation(alias) }
    }

    suspend fun getCurrentLocation(): Location? = locationProvider.getCurrentLocation()
}
