package dev.arrase.geotify.data

object SettingsDefaults {
    val APP_THEME = ThemeSetting.SYSTEM
    val MAP_THEME = ThemeSetting.SYSTEM
    const val OUTER_RADIUS_N = 5.0f
    const val INNER_RADIUS_R = 3.0f
    const val LOCATION_CACHE_TIMEOUT_SECS = 30
    const val RECALCULATION_DEBOUNCE_SECS = 8
    const val MASTER_GEOFENCE_RESPONSIVENESS_SECS = 60
    const val POI_GEOFENCE_RESPONSIVENESS_SECS = 10
}
