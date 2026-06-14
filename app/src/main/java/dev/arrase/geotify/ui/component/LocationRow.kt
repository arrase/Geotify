package dev.arrase.geotify.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.arrase.geotify.data.entity.LocationEntity
import java.util.Locale

@Composable
fun LocationRow(
    location: LocationEntity,
    activeReminderCount: Int,
    modifier: Modifier = Modifier
) {
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
            if (activeReminderCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(activeReminderCount.toString())
                }
            }
        },
        modifier = modifier
    )
}
