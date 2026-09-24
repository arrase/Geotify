package dev.arrase.geotify.ui.screen

import android.location.Location
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.location.LocationProvider
import dev.arrase.geotify.ui.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsViewModelTest {

    private val locationRepository: LocationRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val geofenceOrchestrator: GeofenceOrchestrator = mock()
    private val locationProvider: LocationProvider = mock()
    private val settingsManager: SettingsManager = mock()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: LocationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(locationRepository.observeLocations()).thenReturn(flowOf(emptyList()))
        whenever(reminderRepository.observeActiveReminderCounts()).thenReturn(flowOf(emptyList()))
        whenever(settingsManager.mapTheme).thenReturn(flowOf(SettingsDefaults.MAP_THEME))

        viewModel = LocationsViewModel(
            locationRepository = locationRepository,
            reminderRepository = reminderRepository,
            geofenceOrchestrator = geofenceOrchestrator,
            locationProvider = locationProvider,
            settingsManager = settingsManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── StateFlows initialization tests ──

    @Test
    fun locations_emitsFlowFromRepository() {
        runTest {
            val entities = listOf(LocationEntity("1", "Casa", 40.0, -3.0))
            whenever(locationRepository.observeLocations()).thenReturn(flowOf(entities))

            val vm = LocationsViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.locations.collect {} }

            assertEquals(entities, vm.locations.value)
        }
    }

    @Test
    fun activeReminderCounts_mapsAndEmitsCountsFromRepository() {
        runTest {
            val counts = listOf(
                LocationReminderCount("loc-1", 3),
                LocationReminderCount("loc-2", 1)
            )
            whenever(reminderRepository.observeActiveReminderCounts()).thenReturn(flowOf(counts))

            val vm = LocationsViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.activeReminderCounts.collect {} }

            val expected = mapOf("loc-1" to 3, "loc-2" to 1)
            assertEquals(expected, vm.activeReminderCounts.value)
        }
    }

    @Test
    fun mapTheme_emitsThemeFromSettings() {
        runTest {
            whenever(settingsManager.mapTheme).thenReturn(flowOf(ThemeSetting.DARK))

            val vm = LocationsViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.mapTheme.collect {} }

            assertEquals(ThemeSetting.DARK, vm.mapTheme.value)
        }
    }

    // ── saveLocation tests ──

    @Test
    fun saveLocation_success_callsRepositoryAndTriggersRecalculation() {
        runTest {
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.saveLocation("Casa", 40.0, -3.0, 150f, 1000)

            verify(locationRepository).saveLocation("Casa", 40.0, -3.0, 150f, 1000)
            verify(geofenceOrchestrator).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun saveLocation_onRepositoryException_emitsErrorMessageToSnackbar() {
        runTest {
            whenever(locationRepository.saveLocation(any(), any(), any(), any(), any()))
                .thenThrow(RuntimeException("Duplicate alias"))
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.saveLocation("Casa", 40.0, -3.0, 150f, 0)

            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Duplicate alias"), messages[0])
        }
    }

    @Test
    fun saveLocation_onExceptionWithoutMessage_emitsDefaultErrorMessage() {
        runTest {
            whenever(locationRepository.saveLocation(any(), any(), any(), any(), any()))
                .thenThrow(RuntimeException())
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.saveLocation("Casa", 40.0, -3.0, 150f, 0)

            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Unknown error saving location"), messages[0])
        }
    }

    // ── updateLocation tests ──

    @Test
    fun updateLocation_success_callsRepositoryAndTriggersRecalculation() {
        runTest {
            val location = LocationEntity("1", "Trabajo", 41.0, 2.0)
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.updateLocation(location)

            verify(locationRepository).updateLocation(location)
            verify(geofenceOrchestrator).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun updateLocation_onRepositoryException_emitsErrorMessageToSnackbar() {
        runTest {
            val location = LocationEntity("1", "Trabajo", 41.0, 2.0)
            whenever(locationRepository.updateLocation(location))
                .thenThrow(RuntimeException("Location not found"))
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.updateLocation(location)

            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Location not found"), messages[0])
        }
    }

    @Test
    fun updateLocation_onExceptionWithoutMessage_emitsDefaultErrorMessage() {
        runTest {
            val location = LocationEntity("1", "Trabajo", 41.0, 2.0)
            whenever(locationRepository.updateLocation(location))
                .thenThrow(RuntimeException())
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.updateLocation(location)

            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Unknown error updating location"), messages[0])
        }
    }

    // ── deleteLocation tests ──

    @Test
    fun deleteLocation_success_callsRepositoryAndTriggersRecalculation() {
        runTest {
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.deleteLocation("Casa")

            verify(locationRepository).deleteLocation("Casa")
            verify(geofenceOrchestrator).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun deleteLocation_onRepositoryException_emitsErrorMessageToSnackbar() {
        runTest {
            whenever(locationRepository.deleteLocation("Casa"))
                .thenThrow(RuntimeException("Delete constraint error"))
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.deleteLocation("Casa")

            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Delete constraint error"), messages[0])
        }
    }

    @Test
    fun deleteLocation_onExceptionWithoutMessage_emitsDefaultErrorMessage() {
        runTest {
            whenever(locationRepository.deleteLocation("Casa"))
                .thenThrow(RuntimeException())
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.deleteLocation("Casa")

            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Unknown error deleting location"), messages[0])
        }
    }

    // ── getCurrentLocation tests ──

    @Test
    fun getCurrentLocation_returnsLocationFromProvider() {
        runTest {
            val mockLocation: Location = mock()
            whenever(locationProvider.getCurrentLocation()).thenReturn(mockLocation)

            val result = viewModel.getCurrentLocation()

            assertEquals(mockLocation, result)
            verify(locationProvider).getCurrentLocation()
        }
    }

    @Test
    fun getCurrentLocation_whenProviderReturnsNull_returnsNull() {
        runTest {
            whenever(locationProvider.getCurrentLocation()).thenReturn(null)

            val result = viewModel.getCurrentLocation()

            assertNull(result)
            verify(locationProvider).getCurrentLocation()
        }
    }
}
