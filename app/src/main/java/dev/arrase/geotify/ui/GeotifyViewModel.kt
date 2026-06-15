package dev.arrase.geotify.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.location.LocationProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeotifyViewModel(
    private val repository: GeotifyRepository,
    private val locationProvider: LocationProvider,
    private val settingsManager: SettingsManager
) : ViewModel() {


    val locations: StateFlow<List<LocationEntity>> = repository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeReminderCounts: StateFlow<Map<String, Int>> = repository.observeActiveReminderCounts()
        .map { list -> list.associate { it.locationId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val appTheme: StateFlow<ThemeSetting> = settingsManager.appTheme
    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme

    fun setAppTheme(theme: ThemeSetting) {
        settingsManager.setAppTheme(theme)
    }

    fun setMapTheme(theme: ThemeSetting) {
        settingsManager.setMapTheme(theme)
    }

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

    fun createReminder(locationId: String, message: String, transitionType: Int) {
        viewModelScope.launch {
            val location = repository.findLocationById(locationId) ?: return@launch
            repository.createReminder(location, message, transitionType)
        }
    }

    fun updateReminder(reminder: ReminderEntity, oldLocationId: String) {
        viewModelScope.launch {
            repository.updateReminder(reminder, oldLocationId)
        }
    }

    fun cancelReminder(reminderId: String) {
        viewModelScope.launch { repository.cancelReminder(reminderId) }
    }

    fun deleteLocation(alias: String) {
        viewModelScope.launch { repository.deleteLocation(alias) }
    }

    suspend fun getCurrentLocation(): Location? = locationProvider.getCurrentLocation()

    class Factory(
        private val repository: GeotifyRepository,
        private val locationProvider: LocationProvider,
        private val settingsManager: SettingsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GeotifyViewModel(repository, locationProvider, settingsManager) as T
    }
}
