package dev.arrase.geotify.ui

import dev.arrase.geotify.data.SettingsDefaults
import dev.arrase.geotify.data.SettingsManager
import dev.arrase.geotify.data.ThemeSetting
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val settingsManager: SettingsManager = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun appTheme_initialValue_matchesDefault() {
        whenever(settingsManager.appTheme).thenReturn(flowOf(SettingsDefaults.APP_THEME))
        val viewModel = MainViewModel(settingsManager)

        assertEquals(SettingsDefaults.APP_THEME, viewModel.appTheme.value)
        verify(settingsManager).appTheme
    }

    @Test
    fun appTheme_emitsThemeFromSettings() = runTest(testDispatcher) {
        whenever(settingsManager.appTheme).thenReturn(flowOf(ThemeSetting.DARK))
        val viewModel = MainViewModel(settingsManager)

        backgroundScope.launch { viewModel.appTheme.collect {} }
        advanceUntilIdle()

        assertEquals(ThemeSetting.DARK, viewModel.appTheme.value)
        verify(settingsManager).appTheme
    }
}
