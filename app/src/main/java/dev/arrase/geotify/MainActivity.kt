package dev.arrase.geotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.permission.PermissionGate
import dev.arrase.geotify.ui.GeotifyViewModel
import dev.arrase.geotify.ui.navigation.GeotifyNavHost
import dev.arrase.geotify.ui.navigation.GeotifyTab
import dev.arrase.geotify.ui.theme.GeotifyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GeotifyViewModel by viewModels {
        val app = application as GeotifyApplication
        GeotifyViewModel.Factory(app.repository, app.locationProvider, app.settingsManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = if (intent.getStringExtra(EXTRA_TAB) == TAB_LOCATIONS) {
            GeotifyTab.Locations
        } else {
            GeotifyTab.Reminders
        }

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val useDarkTheme = when (appTheme) {
                ThemeSetting.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeSetting.LIGHT -> false
                ThemeSetting.DARK -> true
            }

            GeotifyTheme(darkTheme = useDarkTheme) {
                PermissionGate {
                    GeotifyNavHost(
                        viewModel = viewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_REMINDERS = "reminders"
        const val TAB_LOCATIONS = "locations"
    }
}
