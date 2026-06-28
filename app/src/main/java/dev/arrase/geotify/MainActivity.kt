package dev.arrase.geotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.permission.PermissionGate
import dev.arrase.geotify.ui.MainViewModel
import dev.arrase.geotify.ui.navigation.GeotifyNavHost
import dev.arrase.geotify.ui.navigation.GeotifyTab
import dev.arrase.geotify.ui.theme.GeotifyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = if (intent.getStringExtra(EXTRA_TAB) == TAB_LOCATIONS) {
            GeotifyTab.Locations
        } else {
            GeotifyTab.Reminders
        }

        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val useDarkTheme = when (appTheme) {
                ThemeSetting.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
            }

            GeotifyTheme(darkTheme = useDarkTheme) {
                PermissionGate {
                    GeotifyNavHost(initialTab = initialTab)
                }
            }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_LOCATIONS = "locations"
    }
}
