package dev.arrase.geotify.ui.screen

import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settingsManager: SettingsManager = mock()
    private val geofenceOrchestrator: GeofenceOrchestrator = mock()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(settingsManager.appTheme).thenReturn(flowOf(SettingsDefaults.APP_THEME))
        whenever(settingsManager.mapTheme).thenReturn(flowOf(SettingsDefaults.MAP_THEME))
        whenever(settingsManager.outerRadiusN).thenReturn(flowOf(SettingsDefaults.OUTER_RADIUS_N))
        whenever(settingsManager.innerRadiusR).thenReturn(flowOf(SettingsDefaults.INNER_RADIUS_R))
        whenever(settingsManager.locationCacheTimeoutSecs).thenReturn(flowOf(SettingsDefaults.LOCATION_CACHE_TIMEOUT_SECS))
        whenever(settingsManager.recalculationDebounceSecs).thenReturn(flowOf(SettingsDefaults.RECALCULATION_DEBOUNCE_SECS))
        whenever(settingsManager.masterGeofenceResponsivenessSecs).thenReturn(flowOf(SettingsDefaults.MASTER_GEOFENCE_RESPONSIVENESS_SECS))
        whenever(settingsManager.poiGeofenceResponsivenessSecs).thenReturn(flowOf(SettingsDefaults.POI_GEOFENCE_RESPONSIVENESS_SECS))

        viewModel = SettingsViewModel(
            settingsManager = settingsManager,
            geofenceOrchestrator = geofenceOrchestrator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial StateFlow Values Tests ──

    @Test
    fun initialValues_matchSettingsDefaults() {
        assertEquals(SettingsDefaults.APP_THEME, viewModel.appTheme.value)
        assertEquals(SettingsDefaults.MAP_THEME, viewModel.mapTheme.value)
        assertEquals(SettingsDefaults.OUTER_RADIUS_N, viewModel.outerRadiusN.value)
        assertEquals(SettingsDefaults.INNER_RADIUS_R, viewModel.innerRadiusR.value)
        assertEquals(SettingsDefaults.LOCATION_CACHE_TIMEOUT_SECS, viewModel.locationCacheTimeoutSecs.value)
        assertEquals(SettingsDefaults.RECALCULATION_DEBOUNCE_SECS, viewModel.recalculationDebounceSecs.value)
        assertEquals(SettingsDefaults.MASTER_GEOFENCE_RESPONSIVENESS_SECS, viewModel.masterGeofenceResponsivenessSecs.value)
        assertEquals(SettingsDefaults.POI_GEOFENCE_RESPONSIVENESS_SECS, viewModel.poiGeofenceResponsivenessSecs.value)
    }

    @Test
    fun appTheme_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.appTheme).thenReturn(flowOf(ThemeSetting.DARK))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.appTheme.collect {} }
        advanceUntilIdle()

        assertEquals(ThemeSetting.DARK, vm.appTheme.value)
    }

    @Test
    fun mapTheme_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.mapTheme).thenReturn(flowOf(ThemeSetting.LIGHT))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.mapTheme.collect {} }
        advanceUntilIdle()

        assertEquals(ThemeSetting.LIGHT, vm.mapTheme.value)
    }

    @Test
    fun outerRadiusN_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.outerRadiusN).thenReturn(flowOf(5000f))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.outerRadiusN.collect {} }
        advanceUntilIdle()

        assertEquals(5000f, vm.outerRadiusN.value)
    }

    @Test
    fun innerRadiusR_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.innerRadiusR).thenReturn(flowOf(1500f))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.innerRadiusR.collect {} }
        advanceUntilIdle()

        assertEquals(1500f, vm.innerRadiusR.value)
    }

    @Test
    fun locationCacheTimeoutSecs_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.locationCacheTimeoutSecs).thenReturn(flowOf(60))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.locationCacheTimeoutSecs.collect {} }
        advanceUntilIdle()

        assertEquals(60, vm.locationCacheTimeoutSecs.value)
    }

    @Test
    fun recalculationDebounceSecs_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.recalculationDebounceSecs).thenReturn(flowOf(15))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.recalculationDebounceSecs.collect {} }
        advanceUntilIdle()

        assertEquals(15, vm.recalculationDebounceSecs.value)
    }

    @Test
    fun masterGeofenceResponsivenessSecs_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.masterGeofenceResponsivenessSecs).thenReturn(flowOf(30))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.masterGeofenceResponsivenessSecs.collect {} }
        advanceUntilIdle()

        assertEquals(30, vm.masterGeofenceResponsivenessSecs.value)
    }

    @Test
    fun poiGeofenceResponsivenessSecs_emitsValueFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.poiGeofenceResponsivenessSecs).thenReturn(flowOf(45))
        val vm = SettingsViewModel(settingsManager, geofenceOrchestrator)
        backgroundScope.launch { vm.poiGeofenceResponsivenessSecs.collect {} }
        advanceUntilIdle()

        assertEquals(45, vm.poiGeofenceResponsivenessSecs.value)
    }

    // ── Setter Methods Tests ──

    @Test
    fun setAppTheme_callsSettingsManager() = runTest(testDispatcher) {
        viewModel.setAppTheme(ThemeSetting.DARK)
        advanceUntilIdle()

        verify(settingsManager).setAppTheme(ThemeSetting.DARK)
        verify(geofenceOrchestrator, never()).triggerRecalculation()
    }

    @Test
    fun setMapTheme_callsSettingsManager() = runTest(testDispatcher) {
        viewModel.setMapTheme(ThemeSetting.LIGHT)
        advanceUntilIdle()

        verify(settingsManager).setMapTheme(ThemeSetting.LIGHT)
        verify(geofenceOrchestrator, never()).triggerRecalculation()
    }

    @Test
    fun setOuterRadiusN_callsSettingsManagerAndTriggersRecalculation() = runTest(testDispatcher) {
        viewModel.setOuterRadiusN(3000f)
        advanceUntilIdle()

        verify(settingsManager).setOuterRadiusN(3000f)
        verify(geofenceOrchestrator).triggerRecalculation()
    }

    @Test
    fun setInnerRadiusR_callsSettingsManagerAndTriggersRecalculation() = runTest(testDispatcher) {
        viewModel.setInnerRadiusR(1000f)
        advanceUntilIdle()

        verify(settingsManager).setInnerRadiusR(1000f)
        verify(geofenceOrchestrator).triggerRecalculation()
    }

    @Test
    fun setLocationCacheTimeoutSecs_callsSettingsManager() = runTest(testDispatcher) {
        viewModel.setLocationCacheTimeoutSecs(45)
        advanceUntilIdle()

        verify(settingsManager).setLocationCacheTimeoutSecs(45)
        verify(geofenceOrchestrator, never()).triggerRecalculation()
    }

    @Test
    fun setRecalculationDebounceSecs_callsSettingsManager() = runTest(testDispatcher) {
        viewModel.setRecalculationDebounceSecs(20)
        advanceUntilIdle()

        verify(settingsManager).setRecalculationDebounceSecs(20)
        verify(geofenceOrchestrator, never()).triggerRecalculation()
    }

    @Test
    fun setMasterGeofenceResponsivenessSecs_callsSettingsManagerAndTriggersRecalculation() = runTest(testDispatcher) {
        viewModel.setMasterGeofenceResponsivenessSecs(60)
        advanceUntilIdle()

        verify(settingsManager).setMasterGeofenceResponsivenessSecs(60)
        verify(geofenceOrchestrator).triggerRecalculation()
    }

    @Test
    fun setPoiGeofenceResponsivenessSecs_callsSettingsManagerAndTriggersRecalculation() = runTest(testDispatcher) {
        viewModel.setPoiGeofenceResponsivenessSecs(90)
        advanceUntilIdle()

        verify(settingsManager).setPoiGeofenceResponsivenessSecs(90)
        verify(geofenceOrchestrator).triggerRecalculation()
    }
}
