package dev.arrase.geonotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.arrase.geonotes.permission.PermissionGate
import dev.arrase.geonotes.ui.GeoNotesViewModel
import dev.arrase.geonotes.ui.navigation.GeoNotesNavHost
import dev.arrase.geonotes.ui.navigation.GeoNotesTab
import dev.arrase.geonotes.ui.theme.GeoNotesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GeoNotesViewModel by viewModels {
        GeoNotesViewModel.Factory((application as GeoNotesApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = if (intent.getStringExtra("tab") == "reminders") {
            GeoNotesTab.Reminders
        } else {
            GeoNotesTab.Locations
        }

        setContent {
            GeoNotesTheme {
                PermissionGate {
                    GeoNotesNavHost(
                        viewModel = viewModel,
                        initialTab = initialTab
                    )
                }
            }
        }
    }
}
