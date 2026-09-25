package dev.arrase.geotify.geofence

import android.content.Context
import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GeofenceOrchestratorTest {

    private val context: Context = mock()
    private val settingsManager: SettingsManager = mock()
    private lateinit var orchestrator: GeofenceOrchestrator

    @Before
    fun setUp() {
        whenever(settingsManager.recalculationDebounceSecs)
            .thenReturn(flowOf(SettingsDefaults.RECALCULATION_DEBOUNCE_SECS))
        orchestrator = GeofenceOrchestrator(context, settingsManager)
    }

    @Test
    fun triggerExpeditedRecalculation_completesWithoutException() {
        orchestrator.triggerExpeditedRecalculation()
    }

    @Test
    fun triggerRecalculation_readsDebounceFromSettingsAndCompletes() = runTest {
        whenever(settingsManager.recalculationDebounceSecs).thenReturn(flowOf(15))

        orchestrator.triggerRecalculation()

        verify(settingsManager).recalculationDebounceSecs
    }
}
