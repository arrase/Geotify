package dev.arrase.geotify.ui

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GeotifyViewModel(private val repository: GeotifyRepository) : ViewModel() {

    val locations: StateFlow<List<LocationEntity>> = repository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun activeReminderCount(locationId: String): Flow<Int> =
        repository.observeActiveReminderCount(locationId)

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

    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cancellationSource = CancellationTokenSource()
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationSource.token
            ).await()
        } catch (e: Exception) {
            null
        }
    }

    class Factory(private val repository: GeotifyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GeotifyViewModel(repository) as T
    }
}
