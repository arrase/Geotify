package dev.arrase.geonotes.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
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
import dev.arrase.geonotes.ui.GeoNotesViewModel
import dev.arrase.geonotes.ui.screen.LocationsScreen
import dev.arrase.geonotes.ui.screen.RemindersScreen

enum class GeoNotesTab(
    val icon: ImageVector,
    val label: String
) {
    Locations(Icons.Filled.LocationOn, "Locations"),
    Reminders(Icons.Filled.Notifications, "Reminders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoNotesNavHost(
    viewModel: GeoNotesViewModel,
    initialTab: GeoNotesTab = GeoNotesTab.Locations
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("GeoNotes") },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                GeoNotesTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
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
                GeoNotesTab.Locations -> LocationsScreen(viewModel)
                GeoNotesTab.Reminders -> RemindersScreen(viewModel)
            }
        }
    }
}
