package dev.arrase.geotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "geotify_settings")

@Singleton
class SettingsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val appTheme: Flow<ThemeSetting> = context.dataStore.data
        .map { preferences ->
            val name = preferences[KEY_APP_THEME] ?: ThemeSetting.SYSTEM.name
            try {
                ThemeSetting.valueOf(name)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM
            }
        }

    val mapTheme: Flow<ThemeSetting> = context.dataStore.data
        .map { preferences ->
            val name = preferences[KEY_MAP_THEME] ?: ThemeSetting.SYSTEM.name
            try {
                ThemeSetting.valueOf(name)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM
            }
        }

    val outerRadiusN: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_OUTER_RADIUS_N] ?: 5.0f }

    val innerRadiusR: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_INNER_RADIUS_R] ?: 4.0f }

    suspend fun setAppTheme(theme: ThemeSetting) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME] = theme.name
        }
    }

    suspend fun setMapTheme(theme: ThemeSetting) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MAP_THEME] = theme.name
        }
    }

    suspend fun setOuterRadiusN(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_OUTER_RADIUS_N] = radius
        }
    }

    suspend fun setInnerRadiusR(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_INNER_RADIUS_R] = radius
        }
    }

    companion object {
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_MAP_THEME = stringPreferencesKey("map_theme")
        private val KEY_OUTER_RADIUS_N = floatPreferencesKey("outer_radius_n_km")
        private val KEY_INNER_RADIUS_R = floatPreferencesKey("inner_radius_r_km")
    }
}
