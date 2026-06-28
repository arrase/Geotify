package dev.arrase.geotify

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.geofence.AndroidGeofenceManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import com.google.android.gms.location.Geofence

@RunWith(AndroidJUnit4::class)
class GeofenceRegistrationTest {

    @Test
    fun testRegisterGeofence() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val settingsManager = dev.arrase.geotify.data.SettingsManager(appContext)
        val manager = AndroidGeofenceManager(appContext, settingsManager)
        val location = LocationEntity(
            id = "test_location_id",
            alias = "TestLocation",
            latitude = 39.950914,
            longitude = -0.062596,
            radiusMeters = 100f
        )
        try {
            manager.registerGeofenceForLocation(location, Geofence.GEOFENCE_TRANSITION_ENTER)
            println("Geofence registered successfully in test")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
