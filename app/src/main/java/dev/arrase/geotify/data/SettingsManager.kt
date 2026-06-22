package dev.arrase.geotify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.doublePreferencesKey
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

    // ── Read Preferences ──

    val appTheme: Flow<ThemeSetting> = preference(KEY_APP_THEME, ThemeSetting.SYSTEM.name)
        .map { name -> ThemeSetting.entries.find { it.name == name } ?: ThemeSetting.SYSTEM }

    val mapTheme: Flow<ThemeSetting> = preference(KEY_MAP_THEME, ThemeSetting.SYSTEM.name)
        .map { name -> ThemeSetting.entries.find { it.name == name } ?: ThemeSetting.SYSTEM }

    val outerRadiusN: Flow<Float> = preference(KEY_OUTER_RADIUS_N, 5.0f)

    val innerRadiusR: Flow<Float> = preference(KEY_INNER_RADIUS_R, 3.0f)

    val locationCacheTimeoutSecs: Flow<Int> = preference(KEY_LOCATION_CACHE_TIMEOUT_SECS, 30)

    val recalculationDebounceSecs: Flow<Int> = preference(KEY_DEBOUNCE_DELAY_SECS, 8)

    val masterGeofenceResponsivenessSecs: Flow<Int> = preference(KEY_MASTER_RESPONSIVENESS_SECS, 60)

    val poiGeofenceResponsivenessSecs: Flow<Int> = preference(KEY_POI_RESPONSIVENESS_SECS, 10)

    val lastRecalcLat: Flow<Double?> = preference(KEY_LAST_RECALC_LAT, 0.0)
        .map { if (it == 0.0) null else it }

    val lastRecalcLng: Flow<Double?> = preference(KEY_LAST_RECALC_LNG, 0.0)
        .map { if (it == 0.0) null else it }

    // ── Write Preferences ──

    suspend fun setAppTheme(theme: ThemeSetting) = setPreference(KEY_APP_THEME, theme.name)

    suspend fun setMapTheme(theme: ThemeSetting) = setPreference(KEY_MAP_THEME, theme.name)

    suspend fun setOuterRadiusN(radius: Float) = setPreference(KEY_OUTER_RADIUS_N, radius)

    suspend fun setInnerRadiusR(radius: Float) = setPreference(KEY_INNER_RADIUS_R, radius)

    suspend fun setLocationCacheTimeoutSecs(secs: Int) = setPreference(KEY_LOCATION_CACHE_TIMEOUT_SECS, secs)

    suspend fun setRecalculationDebounceSecs(secs: Int) = setPreference(KEY_DEBOUNCE_DELAY_SECS, secs)

    suspend fun setMasterGeofenceResponsivenessSecs(secs: Int) = setPreference(KEY_MASTER_RESPONSIVENESS_SECS, secs)

    suspend fun setPoiGeofenceResponsivenessSecs(secs: Int) = setPreference(KEY_POI_RESPONSIVENESS_SECS, secs)

    suspend fun setLastRecalcLocation(lat: Double, lng: Double) {
        setPreference(KEY_LAST_RECALC_LAT, lat)
        setPreference(KEY_LAST_RECALC_LNG, lng)
    }

    // ── Private Helpers ──

    private fun <T> preference(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Error reading preference: $key", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> preferences[key] ?: default }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing preference: $key", e)
        }
    }

    companion object {
        private const val TAG = "SettingsManager"
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_MAP_THEME = stringPreferencesKey("map_theme")
        private val KEY_OUTER_RADIUS_N = floatPreferencesKey("outer_radius_n_km")
        private val KEY_INNER_RADIUS_R = floatPreferencesKey("inner_radius_r_km")
        private val KEY_LOCATION_CACHE_TIMEOUT_SECS = intPreferencesKey("location_cache_timeout_secs")
        private val KEY_DEBOUNCE_DELAY_SECS = intPreferencesKey("debounce_delay_secs")
        private val KEY_MASTER_RESPONSIVENESS_SECS = intPreferencesKey("master_responsiveness_secs")
        private val KEY_POI_RESPONSIVENESS_SECS = intPreferencesKey("poi_responsiveness_secs")
        private val KEY_LAST_RECALC_LAT = doublePreferencesKey("last_recalc_lat")
        private val KEY_LAST_RECALC_LNG = doublePreferencesKey("last_recalc_lng")
    }
}
