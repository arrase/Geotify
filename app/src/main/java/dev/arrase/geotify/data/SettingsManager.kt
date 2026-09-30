package dev.arrase.geotify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
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

    val appTheme: Flow<ThemeSetting> = preference(KEY_APP_THEME, SettingsDefaults.APP_THEME.name)
        .map { name -> ThemeSetting.entries.find { it.name == name } ?: SettingsDefaults.APP_THEME }

    val mapTheme: Flow<ThemeSetting> = preference(KEY_MAP_THEME, SettingsDefaults.MAP_THEME.name)
        .map { name -> ThemeSetting.entries.find { it.name == name } ?: SettingsDefaults.MAP_THEME }

    val outerRadiusN: Flow<Float> = preference(KEY_OUTER_RADIUS_N, SettingsDefaults.OUTER_RADIUS_N)

    val innerRadiusR: Flow<Float> = preference(KEY_INNER_RADIUS_R, SettingsDefaults.INNER_RADIUS_R)

    val locationCacheTimeoutSecs: Flow<Int> =
        preference(KEY_LOCATION_CACHE_TIMEOUT_SECS, SettingsDefaults.LOCATION_CACHE_TIMEOUT_SECS)

    val recalculationDebounceSecs: Flow<Int> =
        preference(KEY_DEBOUNCE_DELAY_SECS, SettingsDefaults.RECALCULATION_DEBOUNCE_SECS)

    val masterGeofenceResponsivenessSecs: Flow<Int> =
        preference(KEY_MASTER_RESPONSIVENESS_SECS, SettingsDefaults.MASTER_GEOFENCE_RESPONSIVENESS_SECS)

    val poiGeofenceResponsivenessSecs: Flow<Int> =
        preference(KEY_POI_RESPONSIVENESS_SECS, SettingsDefaults.POI_GEOFENCE_RESPONSIVENESS_SECS)

    /**
     * Centre of the most recent geofence recalculation, or `null` if none has run yet.
     * Backed by an explicit presence flag so that valid coordinates such as (0.0, 0.0)
     * are not mistaken for "unset".
     */
    val lastRecalcLocation: Flow<LatLng?> = context.dataStore.data
        .catch { exception -> recoverFrom(exception, KEY_HAS_LAST_RECALC) }
        .map { preferences ->
            // The flag and both coordinates are written in one atomic `edit`, so when the flag
            // is set the coordinates are guaranteed to be present.
            if (preferences[KEY_HAS_LAST_RECALC] != true) {
                null
            } else {
                LatLng(
                    latitude = checkNotNull(preferences[KEY_LAST_RECALC_LAT]),
                    longitude = checkNotNull(preferences[KEY_LAST_RECALC_LNG])
                )
            }
        }

    // ── Write Preferences ──

    suspend fun setAppTheme(theme: ThemeSetting) = setPreference(KEY_APP_THEME, theme.name)

    suspend fun setMapTheme(theme: ThemeSetting) = setPreference(KEY_MAP_THEME, theme.name)

    suspend fun setOuterRadiusN(radius: Float) = setPreference(KEY_OUTER_RADIUS_N, radius)

    suspend fun setInnerRadiusR(radius: Float) = setPreference(KEY_INNER_RADIUS_R, radius)

    suspend fun setLocationCacheTimeoutSecs(secs: Int) =
        setPreference(KEY_LOCATION_CACHE_TIMEOUT_SECS, secs)

    suspend fun setRecalculationDebounceSecs(secs: Int) = setPreference(KEY_DEBOUNCE_DELAY_SECS, secs)

    suspend fun setMasterGeofenceResponsivenessSecs(secs: Int) =
        setPreference(KEY_MASTER_RESPONSIVENESS_SECS, secs)

    suspend fun setPoiGeofenceResponsivenessSecs(secs: Int) =
        setPreference(KEY_POI_RESPONSIVENESS_SECS, secs)

    /** Stores both coordinates in a single atomic transaction. */
    suspend fun setLastRecalcLocation(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_RECALC_LAT] = latitude
            preferences[KEY_LAST_RECALC_LNG] = longitude
            preferences[KEY_HAS_LAST_RECALC] = true
        }
    }

    // ── Private Helpers ──

    private fun <T> preference(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data
            .catch { exception -> recoverFrom(exception, key) }
            .map { preferences -> preferences[key] ?: default }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    /**
     * Swallows the corruption error thrown when the DataStore file cannot be parsed, so a single
     * unreadable preference does not permanently break the app. Any other failure propagates.
     */
    private suspend fun FlowCollector<Preferences>.recoverFrom(
        exception: Throwable,
        key: Preferences.Key<*>
    ) {
        if (exception is IOException) {
            Log.e(TAG, "Error reading preference: $key", exception)
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    private companion object {
        const val TAG = "SettingsManager"
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_MAP_THEME = stringPreferencesKey("map_theme")
        val KEY_OUTER_RADIUS_N = floatPreferencesKey("outer_radius_n_km")
        val KEY_INNER_RADIUS_R = floatPreferencesKey("inner_radius_r_km")
        val KEY_LOCATION_CACHE_TIMEOUT_SECS = intPreferencesKey("location_cache_timeout_secs")
        val KEY_DEBOUNCE_DELAY_SECS = intPreferencesKey("debounce_delay_secs")
        val KEY_MASTER_RESPONSIVENESS_SECS = intPreferencesKey("master_responsiveness_secs")
        val KEY_POI_RESPONSIVENESS_SECS = intPreferencesKey("poi_responsiveness_secs")
        val KEY_LAST_RECALC_LAT = doublePreferencesKey("last_recalc_lat")
        val KEY_LAST_RECALC_LNG = doublePreferencesKey("last_recalc_lng")
        val KEY_HAS_LAST_RECALC = booleanPreferencesKey("has_last_recalc")
    }
}

/** Immutable latitude/longitude pair. */
data class LatLng(val latitude: Double, val longitude: Double)
