package dev.arrase.geotify.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.ReminderEntity

@Composable
fun ReminderRow(
    reminder: ReminderEntity,
    locationAliasMap: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val alias = locationAliasMap[reminder.locationId] ?: stringResource(R.string.reminder_unknown_location)

    ListItem(
        headlineContent = {
            Column {
                Text(
                    text = alias,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = if (reminder.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        },
        trailingContent = { ReminderStatusChips(reminder) },
        modifier = modifier
    )
}
