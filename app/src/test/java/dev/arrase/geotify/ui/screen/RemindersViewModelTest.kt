package dev.arrase.geotify.ui.screen

import android.location.Location
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.LocationReminderCount
import dev.arrase.geotify.data.entity.ReminderEntity
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
class RemindersViewModelTest {

    private val locationRepository: LocationRepository = mock()
    private val reminderRepository: ReminderRepository = mock()
    private val geofenceOrchestrator: GeofenceOrchestrator = mock()
    private val locationProvider: LocationProvider = mock()
    private val settingsManager: SettingsManager = mock()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: RemindersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(locationRepository.observeLocations()).thenReturn(flowOf(emptyList()))
        whenever(reminderRepository.observeReminders()).thenReturn(flowOf(emptyList()))
        whenever(reminderRepository.observeActiveReminderCounts()).thenReturn(flowOf(emptyList()))
        whenever(settingsManager.mapTheme).thenReturn(flowOf(SettingsDefaults.MAP_THEME))
        whenever(settingsManager.lastRecalcLat).thenReturn(flowOf(null))
        whenever(settingsManager.lastRecalcLng).thenReturn(flowOf(null))
        whenever(settingsManager.innerRadiusR).thenReturn(flowOf(SettingsDefaults.INNER_RADIUS_R))
        whenever(settingsManager.outerRadiusN).thenReturn(flowOf(SettingsDefaults.OUTER_RADIUS_N))

        viewModel = RemindersViewModel(
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
            val entities = listOf(LocationEntity("loc-1", "Casa", 40.0, -3.0))
            whenever(locationRepository.observeLocations()).thenReturn(flowOf(entities))

            val vm = RemindersViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.locations.collect {} }

            assertEquals(entities, vm.locations.value)
        }
    }

    @Test
    fun reminders_emitsFlowFromRepository() {
        runTest {
            val remindersList = listOf(
                ReminderEntity("rem-1", "loc-1", "Comprar leche", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
            )
            whenever(reminderRepository.observeReminders()).thenReturn(flowOf(remindersList))

            val vm = RemindersViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.reminders.collect {} }

            assertEquals(remindersList, vm.reminders.value)
        }
    }

    @Test
    fun activeReminderCounts_mapsAndEmitsCountsFromRepository() {
        runTest {
            val counts = listOf(
                LocationReminderCount("loc-1", 2),
                LocationReminderCount("loc-2", 5)
            )
            whenever(reminderRepository.observeActiveReminderCounts()).thenReturn(flowOf(counts))

            val vm = RemindersViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.activeReminderCounts.collect {} }

            val expected = mapOf("loc-1" to 2, "loc-2" to 5)
            assertEquals(expected, vm.activeReminderCounts.value)
        }
    }

    @Test
    fun settingsFlows_emitConfiguredSettings() {
        runTest {
            whenever(settingsManager.mapTheme).thenReturn(flowOf(ThemeSetting.LIGHT))
            whenever(settingsManager.lastRecalcLat).thenReturn(flowOf(40.4168))
            whenever(settingsManager.lastRecalcLng).thenReturn(flowOf(-3.7038))
            whenever(settingsManager.innerRadiusR).thenReturn(flowOf(10f))
            whenever(settingsManager.outerRadiusN).thenReturn(flowOf(25f))

            val vm = RemindersViewModel(
                locationRepository, reminderRepository, geofenceOrchestrator, locationProvider, settingsManager
            )
            backgroundScope.launch(testDispatcher) { vm.mapTheme.collect {} }
            backgroundScope.launch(testDispatcher) { vm.lastRecalcLat.collect {} }
            backgroundScope.launch(testDispatcher) { vm.lastRecalcLng.collect {} }
            backgroundScope.launch(testDispatcher) { vm.innerRadiusR.collect {} }
            backgroundScope.launch(testDispatcher) { vm.outerRadiusN.collect {} }

            assertEquals(ThemeSetting.LIGHT, vm.mapTheme.value)
            assertEquals(40.4168, vm.lastRecalcLat.value)
            assertEquals(-3.7038, vm.lastRecalcLng.value)
            assertEquals(10f, vm.innerRadiusR.value)
            assertEquals(25f, vm.outerRadiusN.value)
        }
    }

    // ── createReminder tests ──

    @Test
    fun createReminder_whenLocationExists_createsReminderAndTriggersRecalculation() {
        runTest {
            val location = LocationEntity("loc-1", "Super", 40.0, -3.0)
            whenever(locationRepository.findLocationById("loc-1")).thenReturn(location)

            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.createReminder("loc-1", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

            verify(reminderRepository).createReminder(location, "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)
            verify(geofenceOrchestrator).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun createReminder_whenLocationDoesNotExist_returnsEarlyWithoutCreatingOrRecalculating() {
        runTest {
            whenever(locationRepository.findLocationById("non-existent")).thenReturn(null)

            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.createReminder("non-existent", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

            verify(reminderRepository, never()).createReminder(any(), any(), any())
            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun createReminder_onRepositoryException_emitsErrorMessageToSnackbar() {
        runTest {
            val location = LocationEntity("loc-1", "Super", 40.0, -3.0)
            whenever(locationRepository.findLocationById("loc-1")).thenReturn(location)
            whenever(reminderRepository.createReminder(location, "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER))
                .thenThrow(RuntimeException("Reminder insert failed"))

            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.createReminder("loc-1", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Error: Reminder insert failed"), messages[0])
        }
    }

    // ── updateReminder tests ──

    @Test
    fun updateReminder_success_callsRepositoryAndTriggersRecalculation() {
        runTest {
            val reminder = ReminderEntity("rem-1", "loc-1", "Nuevo texto", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 100L)
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.updateReminder(reminder)

            verify(reminderRepository).updateReminder(reminder)
            verify(geofenceOrchestrator).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun updateReminder_onRepositoryException_emitsErrorMessageToSnackbar() {
        runTest {
            val reminder = ReminderEntity("rem-1", "loc-1", "Nuevo texto", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 100L)
            whenever(reminderRepository.updateReminder(reminder))
                .thenThrow(RuntimeException("Update failed"))

            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.updateReminder(reminder)

            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Error: Update failed"), messages[0])
        }
    }

    // ── cancelReminder tests ──

    @Test
    fun cancelReminder_success_callsRepositoryAndTriggersRecalculation() {
        runTest {
            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.cancelReminder("rem-1")

            verify(reminderRepository).cancelReminder("rem-1")
            verify(geofenceOrchestrator).triggerRecalculation()
            assertTrue(messages.isEmpty())
        }
    }

    @Test
    fun cancelReminder_onRepositoryException_emitsErrorMessageToSnackbar() {
        runTest {
            whenever(reminderRepository.cancelReminder("rem-1"))
                .thenThrow(RuntimeException("Cancel failed"))

            val messages = mutableListOf<UiText>()
            backgroundScope.launch(testDispatcher) { viewModel.snackbarMessage.collect { messages.add(it) } }

            viewModel.cancelReminder("rem-1")

            verify(geofenceOrchestrator, never()).triggerRecalculation()
            assertEquals(1, messages.size)
            assertEquals(UiText.DynamicString("Error: Cancel failed"), messages[0])
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
