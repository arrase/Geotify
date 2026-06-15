package dev.arrase.geotify.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("geotify_settings", Context.MODE_PRIVATE)

    private val _appTheme = MutableStateFlow(getAppThemeSetting())
    val appTheme: StateFlow<ThemeSetting> = _appTheme.asStateFlow()

    private val _mapTheme = MutableStateFlow(getMapThemeSetting())
    val mapTheme: StateFlow<ThemeSetting> = _mapTheme.asStateFlow()

    private fun getAppThemeSetting(): ThemeSetting {
        val name = prefs.getString(KEY_APP_THEME, ThemeSetting.SYSTEM.name) ?: ThemeSetting.SYSTEM.name
        return try {
            ThemeSetting.valueOf(name)
        } catch (e: IllegalArgumentException) {
            ThemeSetting.SYSTEM
        }
    }

    private fun getMapThemeSetting(): ThemeSetting {
        val name = prefs.getString(KEY_MAP_THEME, ThemeSetting.SYSTEM.name) ?: ThemeSetting.SYSTEM.name
        return try {
            ThemeSetting.valueOf(name)
        } catch (e: IllegalArgumentException) {
            ThemeSetting.SYSTEM
        }
    }

    fun setAppTheme(theme: ThemeSetting) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
        _appTheme.value = theme
    }

    fun setMapTheme(theme: ThemeSetting) {
        prefs.edit().putString(KEY_MAP_THEME, theme.name).apply()
        _mapTheme.value = theme
    }

    companion object {
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_MAP_THEME = "map_theme"
    }
}
