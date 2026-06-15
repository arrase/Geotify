package dev.arrase.geotify.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import dev.arrase.geotify.R
import dev.arrase.geotify.ui.GeotifyViewModel
import dev.arrase.geotify.ui.screen.LocationsScreen
import dev.arrase.geotify.ui.screen.RemindersScreen
import dev.arrase.geotify.ui.screen.SettingsScreen

enum class GeotifyTab(
    val icon: ImageVector,
    @get:StringRes val labelResId: Int
) {
    Reminders(Icons.Filled.Notifications, R.string.label_reminders),
    Locations(Icons.Filled.LocationOn, R.string.label_locations),
    Settings(Icons.Filled.Settings, R.string.label_settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeotifyNavHost(
    viewModel: GeotifyViewModel,
    initialTab: GeotifyTab = GeotifyTab.Reminders
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Geotify") },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                GeotifyTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelResId)
                            )
                        },
                        label = { Text(stringResource(tab.labelResId)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tabContent"
        ) { tab ->
            when (tab) {
                GeotifyTab.Locations -> LocationsScreen(viewModel)
                GeotifyTab.Reminders -> RemindersScreen(viewModel)
                GeotifyTab.Settings -> SettingsScreen(viewModel)
            }
        }
    }
}
