package dev.arrase.geotify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.arrase.geotify.permission.PermissionGate
import dev.arrase.geotify.ui.GeotifyViewModel
import dev.arrase.geotify.ui.navigation.GeotifyNavHost
import dev.arrase.geotify.ui.navigation.GeotifyTab
import dev.arrase.geotify.ui.theme.GeotifyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GeotifyViewModel by viewModels {
        val app = application as GeotifyApplication
        GeotifyViewModel.Factory(app.repository, app.locationProvider)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = if (intent.getStringExtra("tab") == "reminders") {
            GeotifyTab.Reminders
        } else {
            GeotifyTab.Locations
        }

        setContent {
            GeotifyTheme {
                PermissionGate {
                    GeotifyNavHost(
                        viewModel = viewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }
}
