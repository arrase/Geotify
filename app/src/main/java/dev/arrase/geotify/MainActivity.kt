package dev.arrase.geotify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.arrase.geotify.geofence.GeofenceOrchestrator
import dev.arrase.geotify.permission.PermissionGate
import dev.arrase.geotify.ui.MainViewModel
import dev.arrase.geotify.ui.navigation.GeotifyNavHost
import dev.arrase.geotify.ui.navigation.GeotifyTab
import dev.arrase.geotify.ui.theme.GeotifyTheme
import dev.arrase.geotify.ui.theme.resolve
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var geofenceOrchestrator: GeofenceOrchestrator

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Only on a user-initiated launch: a process started by a broadcast receiver must not
        // spend battery re-registering geofences that the receiver itself is about to refresh.
        if (savedInstanceState == null) {
            geofenceOrchestrator.triggerExpeditedRecalculation()
        }


        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()

            GeotifyTheme(darkTheme = appTheme.resolve(isSystemInDarkTheme())) {
                PermissionGate {
                    GeotifyNavHost(initialTab = requestedTab(intent))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        /** Intent extra carrying a [GeotifyTab] name. */
        const val EXTRA_TAB = "tab"

        private fun requestedTab(intent: Intent?): GeotifyTab {
            val requested = intent?.getStringExtra(EXTRA_TAB) ?: return GeotifyTab.Reminders
            return GeotifyTab.entries.firstOrNull { it.name == requested } ?: GeotifyTab.Reminders
        }
    }
}
