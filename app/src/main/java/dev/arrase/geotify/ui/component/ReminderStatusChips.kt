package dev.arrase.geotify.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.data.entity.isArrival
import dev.arrase.geotify.data.entity.isDeparture

/**
 * Read-only chips describing a reminder's transition and, when it is being monitored, that it is
 * within the current recalculation area. These are status indicators, not controls, so they expose
 * no click behaviour.
 */
@Composable
fun ReminderStatusChips(
    reminder: ReminderEntity,
    modifier: Modifier = Modifier
) {
    val transitionLabel = when {
        reminder.isArrival -> stringResource(R.string.label_transition_arrival)
        reminder.isDeparture -> stringResource(R.string.label_transition_departure)
        else -> stringResource(R.string.label_transition_unknown)
    }

    StatusChip(label = transitionLabel, modifier = modifier)

    if (reminder.isActive && reminder.isInRange) {
        StatusChip(
            label = stringResource(R.string.label_in_range),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    labelColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = labelColor
        ),
        icon = {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        modifier = modifier.height(24.dp)
    )
}
