package dev.arrase.geotify.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.R
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.GeofenceLimitExceededException
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
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
    private val repository: GeotifyRepository
) : ViewModel() {

    private val _snackbarMessage = MutableSharedFlow<UiText>()
    val snackbarMessage: SharedFlow<UiText> = _snackbarMessage.asSharedFlow()

    val locations: StateFlow<List<LocationEntity>> = repository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = repository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeReminderCounts: StateFlow<Map<String, Int>> = repository.observeActiveReminderCounts()
        .map { list -> list.associate { it.locationId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun createReminder(locationId: String, message: String, transitionType: Int) {
        viewModelScope.launch {
            try {
                val location = repository.findLocationById(locationId) ?: return@launch
                val result = repository.createReminder(location, message, transitionType)
                if (result.isLimitWarningTriggered) {
                    _snackbarMessage.emit(UiText.StringResource(R.string.geofence_limit_warning))
                }
            } catch (e: GeofenceLimitExceededException) {
                _snackbarMessage.emit(UiText.StringResource(R.string.geofence_limit_error))
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString("Error: ${e.message}"))
            }
        }
    }

    fun updateReminder(reminder: ReminderEntity, oldLocationId: String) {
        viewModelScope.launch {
            try {
                val warningTriggered = repository.updateReminder(reminder, oldLocationId)
                if (warningTriggered) {
                    _snackbarMessage.emit(UiText.StringResource(R.string.geofence_limit_warning))
                }
            } catch (e: GeofenceLimitExceededException) {
                _snackbarMessage.emit(UiText.StringResource(R.string.geofence_limit_error))
            } catch (e: Exception) {
                _snackbarMessage.emit(UiText.DynamicString("Error: ${e.message}"))
            }
        }
    }

    fun cancelReminder(reminderId: String) {
        viewModelScope.launch { repository.cancelReminder(reminderId) }
    }
}
