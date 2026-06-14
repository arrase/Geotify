package dev.arrase.geotify.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.data.entity.transitionLabel

@Composable
fun ReminderRow(
    reminder: ReminderEntity,
    locationAliasMap: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val alias = locationAliasMap[reminder.locationId] ?: "Unknown"

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
        supportingContent = {
            val statusColor = if (reminder.isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = if (reminder.isActive) "Active" else "Completed",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = statusColor
                ),
                modifier = Modifier.height(24.dp)
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Reminder",
                tint = if (reminder.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        },
        trailingContent = {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = reminder.transitionLabel,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize)
                    )
                },
                modifier = Modifier.height(24.dp)
            )
        },
        modifier = modifier
    )
}
