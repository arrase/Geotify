package dev.arrase.geonotes.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.arrase.geonotes.data.entity.LocationEntity
import dev.arrase.geonotes.ui.GeoNotesViewModel
import java.util.Locale

@Composable
fun LocationRow(location: LocationEntity, viewModel: GeoNotesViewModel) {
    val activeCount by viewModel.activeReminderCount(location.id)
        .collectAsState(initial = 0)

    ListItem(
        headlineContent = {
            Text(
                text = location.alias,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        supportingContent = {
            Text(
                text = String.format(
                    Locale.US,
                    "%.5f, %.5f",
                    location.latitude,
                    location.longitude
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            if (activeCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(activeCount.toString())
                }
            }
        }
    )
}
