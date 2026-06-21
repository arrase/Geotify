package dev.arrase.geotify.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val geofenceOrchestrator: GeofenceOrchestrator
) : ViewModel() {

    val appTheme: StateFlow<ThemeSetting> = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    val outerRadiusN: StateFlow<Float> = settingsManager.outerRadiusN
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5.0f)

    val innerRadiusR: StateFlow<Float> = settingsManager.innerRadiusR
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 4.0f)

    val locationCacheTimeoutSecs: StateFlow<Int> = settingsManager.locationCacheTimeoutSecs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 120)

    val recalculationDebounceSecs: StateFlow<Int> = settingsManager.recalculationDebounceSecs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    val masterGeofenceResponsivenessSecs: StateFlow<Int> = settingsManager.masterGeofenceResponsivenessSecs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 120)

    val poiGeofenceResponsivenessSecs: StateFlow<Int> = settingsManager.poiGeofenceResponsivenessSecs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

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
            geofenceOrchestrator.triggerRecalculation()
        }
    }

    fun setInnerRadiusR(radius: Float) {
        viewModelScope.launch {
            settingsManager.setInnerRadiusR(radius)
            geofenceOrchestrator.triggerRecalculation()
        }
    }

    fun setLocationCacheTimeoutSecs(secs: Int) {
        viewModelScope.launch {
            settingsManager.setLocationCacheTimeoutSecs(secs)
        }
    }

    fun setRecalculationDebounceSecs(secs: Int) {
        viewModelScope.launch {
            settingsManager.setRecalculationDebounceSecs(secs)
        }
    }

    fun setMasterGeofenceResponsivenessSecs(secs: Int) {
        viewModelScope.launch {
            settingsManager.setMasterGeofenceResponsivenessSecs(secs)
            geofenceOrchestrator.triggerRecalculation()
        }
    }

    fun setPoiGeofenceResponsivenessSecs(secs: Int) {
        viewModelScope.launch {
            settingsManager.setPoiGeofenceResponsivenessSecs(secs)
            geofenceOrchestrator.triggerRecalculation()
        }
    }
}
