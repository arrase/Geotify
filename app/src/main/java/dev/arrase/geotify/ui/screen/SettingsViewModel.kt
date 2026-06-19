package dev.arrase.geotify.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.data.GeotifyRepository
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val repository: GeotifyRepository
) : ViewModel() {

    val appTheme: StateFlow<ThemeSetting> = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    val outerRadiusN: StateFlow<Float> = settingsManager.outerRadiusN
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5.0f)

    val innerRadiusR: StateFlow<Float> = settingsManager.innerRadiusR
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 4.0f)

    fun setAppTheme(theme: ThemeSetting) {
        viewModelScope.launch {
            settingsManager.setAppTheme(theme)
        }
    }

    fun setMapTheme(theme: ThemeSetting) {
        viewModelScope.launch {
            settingsManager.setMapTheme(theme)
        }
    }

    fun setOuterRadiusN(radius: Float) {
        viewModelScope.launch {
            settingsManager.setOuterRadiusN(radius)
            repository.triggerRecalculation()
        }
    }

    fun setInnerRadiusR(radius: Float) {
        viewModelScope.launch {
            settingsManager.setInnerRadiusR(radius)
            repository.triggerRecalculation()
        }
    }
}
