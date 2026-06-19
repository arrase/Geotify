package dev.arrase.geotify.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.data.entity.isArrival

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
        supportingContent = if (reminder.isActive && reminder.isInRange) {
            {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(R.string.label_in_range),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        } else null,
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = stringResource(R.string.content_description_reminder),
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
                        text = if (reminder.isArrival) {
                            stringResource(R.string.label_transition_arrival)
                        } else {
                            stringResource(R.string.label_transition_departure)
                        },
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
