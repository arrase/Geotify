package dev.arrase.geotify

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.geofence.AndroidGeofenceManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real Play services geofencing client. Play services exposes no way to query the
 * registered set, so these tests assert the contract the app depends on: registration is refused
 * (not silently swallowed) when permissions are missing, and accepted otherwise.
 */
@RunWith(AndroidJUnit4::class)
class GeofenceRegistrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = AndroidGeofenceManager(context, SettingsManager(context))

    private val location = LocationEntity(
        id = "instrumentation_test_location",
        alias = "TestLocation",
        latitude = 39.950914,
        longitude = -0.062596,
        radiusMeters = 100f
    )

    @Test
    fun slidingWindow_isAcceptedByPlayServices_andCanBeCleared() = runBlocking {
        assumeTrue("Location permission not granted", hasLocationPermission())

        try {
            val registered = manager.registerSlidingWindowGeofences(
                locations = mapOf(location to Geofence.GEOFENCE_TRANSITION_ENTER),
                centerLat = location.latitude,
                centerLon = location.longitude,
                innerRadiusMeters = 500f
            )
            assertTrue("GMS rejected the sliding window request", registered)

            // Must not throw when a window is already registered.
            manager.removeAllGeofences()
        } finally {
            // Never leave a geofence behind: it consumes a slot of the 100-per-app budget.
            runCatching { manager.removeAllGeofences() }
        }
    }

    @Test
    fun registrationWithoutPermission_reportsFailureInsteadOfRegistering() = runBlocking {
        assumeTrue("Test requires the permission to be absent", !hasLocationPermission())

        assertFalse(
            "Registration should be refused without permissions",
            manager.registerMasterGeofence(
                centerLat = location.latitude,
                centerLon = location.longitude,
                innerRadiusMeters = 500f
            )
        )
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
