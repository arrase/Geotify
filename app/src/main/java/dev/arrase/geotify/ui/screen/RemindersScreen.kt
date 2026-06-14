package dev.arrase.geotify.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.ui.GeotifyViewModel
import dev.arrase.geotify.ui.component.EmptyState
import dev.arrase.geotify.ui.component.ReminderRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: GeotifyViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val locationAliasMap = remember(locations) {
        locations.associate { it.id to it.alias }
    }

    val activeReminders = remember(reminders) {
        reminders.filter { it.isActive }
    }
    val completedReminders = remember(reminders) {
        reminders.filter { !it.isActive }
    }

    // Dialog State
    var showDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var selectedLocationId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var transitionType by remember { mutableIntStateOf(Geofence.GEOFENCE_TRANSITION_ENTER) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = reminders.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            EmptyState(
                icon = Icons.Filled.Notifications,
                title = "No reminders yet",
                suggestion = "Tap + to create a reminder, or tell Gemini: \"Remind me to buy milk when I arrive at the supermarket\""
            )
        }

        AnimatedVisibility(
            visible = reminders.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (activeReminders.isNotEmpty()) {
                    stickyHeader(key = "header_active") {
                        SectionHeader("Active")
                    }
                    items(
                        items = activeReminders,
                        key = { it.id }
                    ) { reminder ->
                        ActiveReminderItem(
                            reminder = reminder,
                            locationAliasMap = locationAliasMap,
                            onCancel = {
                                viewModel.cancelReminder(reminder.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Reminder cancelled",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onClick = {
                                editingReminder = reminder
                                selectedLocationId = reminder.locationId
                                message = reminder.message
                                transitionType = reminder.transitionType
                                showDialog = true
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (completedReminders.isNotEmpty()) {
                    stickyHeader(key = "header_completed") {
                        SectionHeader("Completed")
                    }
                    items(
                        items = completedReminders,
                        key = { it.id }
                    ) { reminder ->
                        Box(
                            modifier = Modifier
                                .alpha(0.6f)
                                .animateItem()
                                .clickable {
                                    editingReminder = reminder
                                    selectedLocationId = reminder.locationId
                                    message = reminder.message
                                    transitionType = reminder.transitionType
                                    showDialog = true
                                }
                        ) {
                            ReminderRow(
                                reminder = reminder,
                                locationAliasMap = locationAliasMap
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingReminder = null
                selectedLocationId = locations.firstOrNull()?.id ?: ""
                message = ""
                transitionType = Geofence.GEOFENCE_TRANSITION_ENTER
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    editingReminder = null
                },
                title = {
                    Text(
                        text = if (editingReminder == null) "New Reminder" else "Edit Reminder",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (locations.isEmpty()) {
                            Text(
                                text = "No locations available. Please create a location first in the Locations tab.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            val selectedLocation = locations.find { it.id == selectedLocationId }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedLocation?.alias ?: "Select Location",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target Location") },
                                    trailingIcon = {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { dropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    locations.forEach { location ->
                                        DropdownMenuItem(
                                            text = { Text(location.alias) },
                                            onClick = {
                                                selectedLocationId = location.id
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Reminder Message") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Trigger Condition",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isEnter = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER
                                    val isExit = transitionType == Geofence.GEOFENCE_TRANSITION_EXIT

                                    // Arrival button
                                    Surface(
                                        onClick = { transitionType = Geofence.GEOFENCE_TRANSITION_ENTER },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isEnter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        color = if (isEnter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = if (isEnter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Arrival",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isEnter) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Departure button
                                    Surface(
                                        onClick = { transitionType = Geofence.GEOFENCE_TRANSITION_EXIT },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isExit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        color = if (isExit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                                contentDescription = null,
                                                tint = if (isExit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Departure",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isExit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    val isValid = locations.isNotEmpty() && message.isNotBlank() && selectedLocationId.isNotEmpty()
                    Button(
                        onClick = {
                            if (isValid) {
                                if (editingReminder == null) {
                                    viewModel.createReminder(selectedLocationId, message, transitionType)
                                } else {
                                    val oldLocationId = editingReminder!!.locationId
                                    viewModel.updateReminder(
                                        editingReminder!!.copy(
                                            locationId = selectedLocationId,
                                            message = message,
                                            transitionType = transitionType,
                                            isActive = editingReminder!!.isActive
                                        ),
                                        oldLocationId = oldLocationId
                                    )
                                }
                                showDialog = false
                                editingReminder = null
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingReminder != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.cancelReminder(editingReminder!!.id)
                                    showDialog = false
                                    editingReminder = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Reminder deleted",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Delete")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showDialog = false
                                editingReminder = null
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveReminderItem(
    reminder: ReminderEntity,
    locationAliasMap: Map<String, String>,
    onCancel: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onCancel()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier
    ) {
        Box(modifier = Modifier.clickable(onClick = onClick)) {
            ReminderRow(
                reminder = reminder,
                locationAliasMap = locationAliasMap
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
