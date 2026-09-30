package dev.arrase.geotify.ui.screen

import android.location.Location
import dev.arrase.geotify.R
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
import kotlinx.coroutines.runBlocking
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
    private val messages = mutableListOf<UiText>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runBlocking { whenever(locationRepository.deleteLocation(any())).thenReturn(true) }
        whenever(locationRepository.observeLocations()).thenReturn(flowOf(emptyList()))
        whenever(reminderRepository.observeActiveReminderCounts()).thenReturn(flowOf(emptyList()))
        whenever(settingsManager.mapTheme).thenReturn(flowOf(SettingsDefaults.MAP_THEME))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): LocationsViewModel = LocationsViewModel(
        locationRepository = locationRepository,
        reminderRepository = reminderRepository,
        geofenceOrchestrator = geofenceOrchestrator,
        locationProvider = locationProvider,
        settingsManager = settingsManager
    )

    private fun LocationsViewModel.collectingMessages(scope: kotlinx.coroutines.CoroutineScope) =
        scope.launch(testDispatcher) { messagesFlow.collect { messages.add(it) } }

    // ── State exposure ──

    @Test
    fun `locations emits the repository flow`() = runTest {
        val entities = listOf(LocationEntity("1", "Casa", 40.0, -3.0))
        whenever(locationRepository.observeLocations()).thenReturn(flowOf(entities))

        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.locations.collect {} }

        assertEquals(entities, vm.locations.value)
    }

    @Test
    fun `activeReminderCounts maps counts keyed by location id`() = runTest {
        val counts = listOf(LocationReminderCount("loc-1", 3), LocationReminderCount("loc-2", 1))
        whenever(reminderRepository.observeActiveReminderCounts()).thenReturn(flowOf(counts))

        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.activeReminderCounts.collect {} }

        assertEquals(mapOf("loc-1" to 3, "loc-2" to 1), vm.activeReminderCounts.value)
    }

    @Test
    fun `mapTheme emits the configured theme`() = runTest {
        whenever(settingsManager.mapTheme).thenReturn(flowOf(ThemeSetting.DARK))

        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.mapTheme.collect {} }

        assertEquals(ThemeSetting.DARK, vm.mapTheme.value)
    }

    // ── saveLocation ──

    @Test
    fun `saveLocation saves and recalculates`() = runTest {
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.saveLocation("Casa", 40.0, -3.0, 150f, 1000)

        verify(locationRepository).saveLocation("Casa", 40.0, -3.0, 150f, 1000)
        verify(geofenceOrchestrator).triggerRecalculation()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `saveLocation surfaces the failure and skips recalculation`() = runTest {
        whenever(locationRepository.saveLocation(any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("Duplicate alias"))
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.saveLocation("Casa", 40.0, -3.0, 150f, 0)

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not save the location: Duplicate alias")),
            messages
        )
    }

    @Test
    fun `saveLocation falls back to a generic message when the cause has none`() = runTest {
        whenever(locationRepository.saveLocation(any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException())
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.saveLocation("Casa", 40.0, -3.0, 150f, 0)

        assertEquals(listOf(UiText.DynamicString("Could not save the location")), messages)
    }

    @Test
    fun `a message emitted before collection starts is not lost`() = runTest {
        whenever(locationRepository.saveLocation(any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("boom"))
        val vm = buildViewModel()

        // No collector yet: a SharedFlow would drop this event silently.
        vm.saveLocation("Casa", 40.0, -3.0, 150f, 0)
        vm.collectingMessages(backgroundScope)

        assertEquals(listOf(UiText.DynamicString("Could not save the location: boom")), messages)
    }

    // ── updateLocation ──

    @Test
    fun `updateLocation saves and recalculates`() = runTest {
        val location = LocationEntity("1", "Trabajo", 41.0, 2.0)
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.updateLocation(location)

        verify(locationRepository).updateLocation(location)
        verify(geofenceOrchestrator).triggerRecalculation()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `updateLocation surfaces the failure and skips recalculation`() = runTest {
        val location = LocationEntity("1", "Trabajo", 41.0, 2.0)
        whenever(locationRepository.updateLocation(location))
            .thenThrow(RuntimeException("Location not found"))
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.updateLocation(location)

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not update the location: Location not found")),
            messages
        )
    }

    // ── deleteLocation ──

    @Test
    fun `deleteLocation removes by alias, recalculates and confirms`() = runTest {
        val location = LocationEntity("1", "Casa", 40.0, -3.0)
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.deleteLocation(location)

        verify(locationRepository).deleteLocation("Casa")
        verify(geofenceOrchestrator).triggerRecalculation()
        assertEquals(listOf(UiText.StringResource(R.string.location_deleted, "Casa")), messages)
    }

    @Test
    fun `deleteLocation surfaces the failure and skips recalculation`() = runTest {
        whenever(locationRepository.deleteLocation("Casa"))
            .thenThrow(RuntimeException("Delete constraint error"))
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.deleteLocation(LocationEntity("1", "Casa", 40.0, -3.0))

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not delete the location: Delete constraint error")),
            messages
        )
    }

    // ── getCurrentLocation ──

    @Test
    fun `getCurrentLocation delegates to the provider`() = runTest {
        val fix: Location = mock()
        whenever(locationProvider.getCurrentLocation()).thenReturn(fix)

        assertEquals(fix, buildViewModel().getCurrentLocation())
        verify(locationProvider).getCurrentLocation()
    }

    @Test
    fun `getCurrentLocation returns null when no fix is available`() = runTest {
        whenever(locationProvider.getCurrentLocation()).thenReturn(null)

        assertNull(buildViewModel().getCurrentLocation())
    }
}
