package dev.arrase.geotify.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsManager: SettingsManager
) : BaseViewModel() {

    val appTheme: StateFlow<ThemeSetting> =
        settingsManager.appTheme.settingFlow(SettingsDefaults.APP_THEME)
}
