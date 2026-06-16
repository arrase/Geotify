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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.ui.UiText
import dev.arrase.geotify.ui.component.BackgroundLocationWarningBanner
import dev.arrase.geotify.ui.component.DialogDismissButtons
import dev.arrase.geotify.ui.component.EmptyState
import dev.arrase.geotify.ui.component.ReminderRow
import dev.arrase.geotify.ui.component.SwipeToDeleteContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val activeReminderCounts by viewModel.activeReminderCounts.collectAsState()
    val activeGeofencesCount = remember(activeReminderCounts) { activeReminderCounts.size }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { uiText ->
            val msg = when (uiText) {
                is UiText.DynamicString -> uiText.value
                is UiText.StringResource -> context.getString(uiText.resId)
            }
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var selectedLocationId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var transitionType by remember { mutableIntStateOf(Geofence.GEOFENCE_TRANSITION_ENTER) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        BackgroundLocationWarningBanner()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = reminders.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EmptyState(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.empty_reminders_title),
                    suggestion = stringResource(R.string.empty_reminders_suggestion)
                )
            }

        androidx.compose.animation.AnimatedVisibility(
            visible = reminders.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                if (activeReminders.isNotEmpty()) {
                    stickyHeader(key = "header_active") {
                        SectionHeader(stringResource(R.string.label_active))
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
                                        message = context.getString(R.string.toast_reminder_cancelled),
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
                        SectionHeader(stringResource(R.string.label_completed))
                    }
                    items(
                        items = completedReminders,
                        key = { it.id }
                    ) { reminder ->
                        SwipeToDeleteContainer(
                            onDelete = {
                                viewModel.cancelReminder(reminder.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.toast_reminder_deleted),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier
                                .alpha(0.6f)
                                .animateItem()
                        ) {
                            Box(
                                modifier = Modifier.clickable {
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
        }

        FloatingActionButton(
            onClick = {
                if (activeGeofencesCount >= 100) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.geofence_limit_fab_warning),
                            duration = SnackbarDuration.Long
                        )
                    }
                }
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
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_description_add_reminder))
        }

        if (showDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showDialog = false
                    editingReminder = null
                },
                sheetState = sheetState
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (editingReminder == null) stringResource(R.string.dialog_new_reminder) else stringResource(R.string.dialog_edit_reminder),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (locations.isEmpty()) {
                        Text(
                            text = stringResource(R.string.reminder_no_locations),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val selectedLocation = locations.find { it.id == selectedLocationId }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedLocation?.alias ?: stringResource(R.string.reminder_select_location),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.reminder_target_location)) },
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
                            label = { Text(stringResource(R.string.reminder_message)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.reminder_trigger_condition),
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
                                            text = stringResource(R.string.label_arrival),
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
                                            text = stringResource(R.string.label_departure),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isExit) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val selectedLocationHasGeofence = activeReminderCounts.containsKey(selectedLocationId)
                    val exceedsLimit = activeGeofencesCount >= 100 && !selectedLocationHasGeofence
                    val isValid = locations.isNotEmpty() && message.isNotBlank() && selectedLocationId.isNotEmpty() && !exceedsLimit

                    if (exceedsLimit) {
                        Text(
                            text = stringResource(R.string.geofence_limit_dialog_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val editing = editingReminder
                        DialogDismissButtons(
                            isEditing = editing != null,
                            onDelete = {
                                if (editing != null) {
                                    viewModel.cancelReminder(editing.id)
                                    showDialog = false
                                    editingReminder = null
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.toast_reminder_deleted),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onCancel = {
                                showDialog = false
                                editingReminder = null
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (isValid) {
                                    val currentEditing = editingReminder
                                    if (currentEditing == null) {
                                        viewModel.createReminder(selectedLocationId, message, transitionType)
                                    } else {
                                        viewModel.updateReminder(
                                            currentEditing.copy(
                                                locationId = selectedLocationId,
                                                message = message,
                                                transitionType = transitionType
                                            ),
                                            oldLocationId = currentEditing.locationId
                                        )
                                    }
                                    showDialog = false
                                    editingReminder = null
                                }
                            },
                            enabled = isValid,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.btn_save))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
}

@Composable
private fun ActiveReminderItem(
    reminder: ReminderEntity,
    locationAliasMap: Map<String, String>,
    onCancel: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToDeleteContainer(
        onDelete = onCancel,
        contentDescription = stringResource(R.string.content_description_cancel),
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
