package dev.arrase.geotify.ui.screen

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.ui.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val geofenceOrchestrator: GeofenceOrchestrator
) : BaseViewModel() {

    val appTheme: StateFlow<ThemeSetting> =
        settingsManager.appTheme.settingFlow(SettingsDefaults.APP_THEME)

    val mapTheme: StateFlow<ThemeSetting> =
        settingsManager.mapTheme.settingFlow(SettingsDefaults.MAP_THEME)

    val outerRadiusN: StateFlow<Float> =
        settingsManager.outerRadiusN.settingFlow(SettingsDefaults.OUTER_RADIUS_N)

    val innerRadiusR: StateFlow<Float> =
        settingsManager.innerRadiusR.settingFlow(SettingsDefaults.INNER_RADIUS_R)

    val locationCacheTimeoutSecs: StateFlow<Int> = settingsManager.locationCacheTimeoutSecs
        .settingFlow(SettingsDefaults.LOCATION_CACHE_TIMEOUT_SECS)

    val recalculationDebounceSecs: StateFlow<Int> = settingsManager.recalculationDebounceSecs
        .settingFlow(SettingsDefaults.RECALCULATION_DEBOUNCE_SECS)

    val masterGeofenceResponsivenessSecs: StateFlow<Int> =
        settingsManager.masterGeofenceResponsivenessSecs
            .settingFlow(SettingsDefaults.MASTER_GEOFENCE_RESPONSIVENESS_SECS)

    val poiGeofenceResponsivenessSecs: StateFlow<Int> = settingsManager.poiGeofenceResponsivenessSecs
        .settingFlow(SettingsDefaults.POI_GEOFENCE_RESPONSIVENESS_SECS)

    fun setAppTheme(theme: ThemeSetting) = persist {
        settingsManager.setAppTheme(theme)
    }

    fun setMapTheme(theme: ThemeSetting) = persist {
        settingsManager.setMapTheme(theme)
    }

    fun setLocationCacheTimeoutSecs(secs: Int) = persist {
        settingsManager.setLocationCacheTimeoutSecs(secs)
    }

    fun setRecalculationDebounceSecs(secs: Int) = persist {
        settingsManager.setRecalculationDebounceSecs(secs)
    }

    /**
     * The sliding window depends on both radii, so they are written together and the
     * recalculation is triggered once, after both have been persisted.
     */
    fun setRadii(outerRadiusKm: Float, innerRadiusKm: Float) = viewModelScope.launch {
        mutate(ERROR_SAVE, onSuccess = geofenceOrchestrator::triggerRecalculation) {
            settingsManager.setOuterRadiusN(outerRadiusKm)
            settingsManager.setInnerRadiusR(innerRadiusKm)
        }
    }

    fun setMasterGeofenceResponsivenessSecs(secs: Int) = recalculate {
        settingsManager.setMasterGeofenceResponsivenessSecs(secs)
    }

    fun setPoiGeofenceResponsivenessSecs(secs: Int) = recalculate {
        settingsManager.setPoiGeofenceResponsivenessSecs(secs)
    }

    private fun persist(write: suspend () -> Unit) = viewModelScope.launch {
        mutate(ERROR_SAVE, onSuccess = {}, action = write)
    }

    private fun recalculate(write: suspend () -> Unit) = viewModelScope.launch {
        mutate(ERROR_SAVE, onSuccess = geofenceOrchestrator::triggerRecalculation, action = write)
    }

    private companion object {
        const val ERROR_SAVE = "Could not save the setting"
    }
}
