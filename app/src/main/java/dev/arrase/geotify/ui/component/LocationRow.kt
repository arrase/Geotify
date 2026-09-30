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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.LocationEntity

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
                text = stringResource(
                    R.string.coordinates_format,
                    location.latitude,
                    location.longitude
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            // Decorative: the alias is already the first thing a screen reader announces.
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            if (activeReminderCount > 0) {
                val description = pluralStringResource(
                    R.plurals.active_reminder_count,
                    activeReminderCount,
                    activeReminderCount
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = description }
                ) {
                    Text(activeReminderCount.toString())
                }
            }
        },
        modifier = modifier
    )
}
