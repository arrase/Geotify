package dev.arrase.geotify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
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
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading app theme preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[KEY_APP_THEME] ?: ThemeSetting.SYSTEM.name
            try {
                ThemeSetting.valueOf(name)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM
            }
        }

    val mapTheme: Flow<ThemeSetting> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading map theme preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[KEY_MAP_THEME] ?: ThemeSetting.SYSTEM.name
            try {
                ThemeSetting.valueOf(name)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM
            }
        }

    val outerRadiusN: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading outer radius preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[KEY_OUTER_RADIUS_N] ?: 5.0f }

    val innerRadiusR: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading inner radius preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[KEY_INNER_RADIUS_R] ?: 4.0f }

    val locationCacheTimeoutSecs: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading location cache timeout", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[KEY_LOCATION_CACHE_TIMEOUT_SECS] ?: 120 }

    val recalculationDebounceSecs: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading recalculation debounce delay", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[KEY_DEBOUNCE_DELAY_SECS] ?: 5 }

    val masterGeofenceResponsivenessSecs: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading master geofence responsiveness", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[KEY_MASTER_RESPONSIVENESS_SECS] ?: 120 }

    val poiGeofenceResponsivenessSecs: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("SettingsManager", "Error reading POI geofence responsiveness", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[KEY_POI_RESPONSIVENESS_SECS] ?: 30 }

    suspend fun setAppTheme(theme: ThemeSetting) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_APP_THEME] = theme.name
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting app theme", e)
        }
    }

    suspend fun setMapTheme(theme: ThemeSetting) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_MAP_THEME] = theme.name
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting map theme", e)
        }
    }

    suspend fun setOuterRadiusN(radius: Float) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_OUTER_RADIUS_N] = radius
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting outer radius N", e)
        }
    }

    suspend fun setInnerRadiusR(radius: Float) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_INNER_RADIUS_R] = radius
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting inner radius R", e)
        }
    }

    suspend fun setLocationCacheTimeoutSecs(secs: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_LOCATION_CACHE_TIMEOUT_SECS] = secs
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting location cache timeout", e)
        }
    }

    suspend fun setRecalculationDebounceSecs(secs: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_DEBOUNCE_DELAY_SECS] = secs
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting recalculation debounce", e)
        }
    }

    suspend fun setMasterGeofenceResponsivenessSecs(secs: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_MASTER_RESPONSIVENESS_SECS] = secs
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting master geofence responsiveness", e)
        }
    }

    suspend fun setPoiGeofenceResponsivenessSecs(secs: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_POI_RESPONSIVENESS_SECS] = secs
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error setting POI geofence responsiveness", e)
        }
    }

    companion object {
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_MAP_THEME = stringPreferencesKey("map_theme")
        private val KEY_OUTER_RADIUS_N = floatPreferencesKey("outer_radius_n_km")
        private val KEY_INNER_RADIUS_R = floatPreferencesKey("inner_radius_r_km")
        private val KEY_LOCATION_CACHE_TIMEOUT_SECS = intPreferencesKey("location_cache_timeout_secs")
        private val KEY_DEBOUNCE_DELAY_SECS = intPreferencesKey("debounce_delay_secs")
        private val KEY_MASTER_RESPONSIVENESS_SECS = intPreferencesKey("master_responsiveness_secs")
        private val KEY_POI_RESPONSIVENESS_SECS = intPreferencesKey("poi_responsiveness_secs")
    }
}
