package dev.arrase.geotify.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val settingsManager: SettingsManager
) : ViewModel() {

    val appTheme: StateFlow<ThemeSetting> = settingsManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

    val mapTheme: StateFlow<ThemeSetting> = settingsManager.mapTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeSetting.SYSTEM)

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
}
