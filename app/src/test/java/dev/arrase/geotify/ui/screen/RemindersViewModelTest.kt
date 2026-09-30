package dev.arrase.geotify.ui.screen

import android.location.Location
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.R
import dev.arrase.geotify.data.LatLng
import dev.arrase.geotify.data.LocationRepository
import dev.arrase.geotify.data.ReminderRepository
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.location.LocationProvider
import dev.arrase.geotify.ui.UiText
import kotlinx.coroutines.CoroutineScope
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
class RemindersViewModelTest {

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
        runBlocking { whenever(reminderRepository.cancelReminder(any())).thenReturn(true) }
        whenever(locationRepository.observeLocations()).thenReturn(flowOf(emptyList()))
        whenever(reminderRepository.observeReminders()).thenReturn(flowOf(emptyList()))
        whenever(settingsManager.mapTheme).thenReturn(flowOf(SettingsDefaults.MAP_THEME))
        whenever(settingsManager.lastRecalcLocation).thenReturn(flowOf(null))
        whenever(settingsManager.innerRadiusR).thenReturn(flowOf(SettingsDefaults.INNER_RADIUS_R))
        whenever(settingsManager.outerRadiusN).thenReturn(flowOf(SettingsDefaults.OUTER_RADIUS_N))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): RemindersViewModel = RemindersViewModel(
        locationRepository = locationRepository,
        reminderRepository = reminderRepository,
        geofenceOrchestrator = geofenceOrchestrator,
        locationProvider = locationProvider,
        settingsManager = settingsManager
    )

    private fun RemindersViewModel.collectingMessages(scope: CoroutineScope) =
        scope.launch(testDispatcher) { messagesFlow.collect { messages.add(it) } }

    // ── State exposure ──

    @Test
    fun `locations emits the repository flow`() = runTest {
        val entities = listOf(LocationEntity("loc-1", "Casa", 40.0, -3.0))
        whenever(locationRepository.observeLocations()).thenReturn(flowOf(entities))

        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.locations.collect {} }

        assertEquals(entities, vm.locations.value)
    }

    @Test
    fun `reminders emits the repository flow`() = runTest {
        val reminders = listOf(
            ReminderEntity("rem-1", "loc-1", "Comprar leche", Geofence.GEOFENCE_TRANSITION_ENTER, createdAt = 100L)
        )
        whenever(reminderRepository.observeReminders()).thenReturn(flowOf(reminders))

        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.reminders.collect {} }

        assertEquals(reminders, vm.reminders.value)
    }

    @Test
    fun `settings flows expose their configured values`() = runTest {
        whenever(settingsManager.mapTheme).thenReturn(flowOf(ThemeSetting.LIGHT))
        whenever(settingsManager.lastRecalcLocation).thenReturn(flowOf(LatLng(40.4168, -3.7038)))
        whenever(settingsManager.innerRadiusR).thenReturn(flowOf(10f))
        whenever(settingsManager.outerRadiusN).thenReturn(flowOf(25f))

        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.mapTheme.collect {} }
        backgroundScope.launch(testDispatcher) { vm.lastRecalcLocation.collect {} }
        backgroundScope.launch(testDispatcher) { vm.innerRadiusR.collect {} }
        backgroundScope.launch(testDispatcher) { vm.outerRadiusN.collect {} }

        assertEquals(ThemeSetting.LIGHT, vm.mapTheme.value)
        assertEquals(LatLng(40.4168, -3.7038), vm.lastRecalcLocation.value)
        assertEquals(10f, vm.innerRadiusR.value)
        assertEquals(25f, vm.outerRadiusN.value)
    }

    @Test
    fun `lastRecalcLocation reports null before the first recalculation`() = runTest {
        val vm = buildViewModel()
        backgroundScope.launch(testDispatcher) { vm.lastRecalcLocation.collect {} }

        assertNull(vm.lastRecalcLocation.value)
    }

    // ── createReminder ──

    @Test
    fun `createReminder creates and recalculates when the location exists`() = runTest {
        val location = LocationEntity("loc-1", "Super", 40.0, -3.0)
        whenever(locationRepository.findLocationById("loc-1")).thenReturn(location)
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.createReminder("loc-1", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

        verify(reminderRepository).createReminder(location, "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)
        verify(geofenceOrchestrator).triggerRecalculation()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `createReminder reports a missing location instead of failing silently`() = runTest {
        whenever(locationRepository.findLocationById("gone")).thenReturn(null)
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.createReminder("gone", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

        verify(reminderRepository, never()).createReminder(any(), any(), any())
        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(listOf(UiText.StringResource(R.string.err_location_missing)), messages)
    }

    @Test
    fun `createReminder surfaces the failure and skips recalculation`() = runTest {
        val location = LocationEntity("loc-1", "Super", 40.0, -3.0)
        whenever(locationRepository.findLocationById("loc-1")).thenReturn(location)
        whenever(reminderRepository.createReminder(location, "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER))
            .thenThrow(RuntimeException("Reminder insert failed"))
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.createReminder("loc-1", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not create the reminder: Reminder insert failed")),
            messages
        )
    }

    @Test
    fun `createReminder falls back to a generic message when the cause has none`() = runTest {
        val location = LocationEntity("loc-1", "Super", 40.0, -3.0)
        whenever(locationRepository.findLocationById("loc-1")).thenReturn(location)
        whenever(reminderRepository.createReminder(location, "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER))
            .thenThrow(RuntimeException())
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.createReminder("loc-1", "Comprar café", Geofence.GEOFENCE_TRANSITION_ENTER)

        assertEquals(listOf(UiText.DynamicString("Could not create the reminder")), messages)
    }

    // ── updateReminder ──

    @Test
    fun `updateReminder saves and recalculates`() = runTest {
        val reminder = ReminderEntity("rem-1", "loc-1", "Nuevo texto", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 100L)
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.updateReminder(reminder)

        verify(reminderRepository).updateReminder(reminder)
        verify(geofenceOrchestrator).triggerRecalculation()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `updateReminder surfaces the failure and skips recalculation`() = runTest {
        val reminder = ReminderEntity("rem-1", "loc-1", "Nuevo texto", Geofence.GEOFENCE_TRANSITION_EXIT, createdAt = 100L)
        whenever(reminderRepository.updateReminder(reminder)).thenThrow(RuntimeException("Update failed"))
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.updateReminder(reminder)

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not update the reminder: Update failed")),
            messages
        )
    }

    // ── cancelReminder ──

    @Test
    fun `cancelReminder cancels, recalculates and confirms`() = runTest {
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.cancelReminder("rem-1")

        verify(reminderRepository).cancelReminder("rem-1")
        verify(geofenceOrchestrator).triggerRecalculation()
        assertEquals(listOf(UiText.StringResource(R.string.reminder_cancelled)), messages)
    }

    @Test
    fun `cancelReminder surfaces the failure and skips recalculation`() = runTest {
        whenever(reminderRepository.cancelReminder("rem-1")).thenThrow(RuntimeException("Cancel failed"))
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.cancelReminder("rem-1")

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not cancel the reminder: Cancel failed")),
            messages
        )
    }

    @Test
    fun `cancelReminder reports a failure when the reminder is already gone`() = runTest {
        runBlocking { whenever(reminderRepository.cancelReminder("gone")).thenReturn(false) }
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.cancelReminder("gone")

        verify(geofenceOrchestrator, never()).triggerRecalculation()
        assertEquals(
            listOf(UiText.DynamicString("Could not cancel the reminder: Reminder gone no longer exists")),
            messages
        )
    }

    @Test
    fun `cancelling an active reminder and deleting a completed one are worded differently`() = runTest {
        val vm = buildViewModel().also { it.collectingMessages(backgroundScope) }

        vm.cancelReminder("rem-1", cancelled = true)
        vm.cancelReminder("rem-2", cancelled = false)

        assertEquals(
            listOf(
                UiText.StringResource(R.string.reminder_cancelled),
                UiText.StringResource(R.string.reminder_deleted)
            ),
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
