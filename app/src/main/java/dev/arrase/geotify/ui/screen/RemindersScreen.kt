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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.Geofence
import dev.arrase.geotify.R
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import dev.arrase.geotify.data.entity.isArrival
import dev.arrase.geotify.ui.UiText
import dev.arrase.geotify.ui.component.BackgroundLocationWarningBanner
import dev.arrase.geotify.ui.component.DialogDismissButtons
import dev.arrase.geotify.ui.component.EmptyState
import dev.arrase.geotify.ui.component.ReminderMapView
import dev.arrase.geotify.ui.component.ReminderMapViewData
import dev.arrase.geotify.ui.component.ReminderRow
import dev.arrase.geotify.ui.component.SpatialRecalculationArea
import dev.arrase.geotify.ui.component.SwipeToDeleteContainer
import dev.arrase.geotify.ui.component.ViewModeSwitcher
import kotlinx.coroutines.launch

private fun isMapDark(themeSetting: ThemeSetting, isSystemDark: Boolean): Boolean = when (themeSetting) {
    ThemeSetting.SYSTEM -> isSystemDark
    ThemeSetting.LIGHT -> false
    ThemeSetting.DARK -> true
}

private fun saveReminder(
    viewModel: RemindersViewModel,
    editingReminder: ReminderEntity?,
    locationId: String,
    message: String,
    transitionType: Int
) {
    if (editingReminder == null) {
        viewModel.createReminder(locationId, message, transitionType)
    } else {
        viewModel.updateReminder(
            editingReminder.copy(
                locationId = locationId,
                message = message,
                transitionType = transitionType
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val mapThemeSetting by viewModel.mapTheme.collectAsStateWithLifecycle()
    val lastRecalcLat by viewModel.lastRecalcLat.collectAsStateWithLifecycle()
    val lastRecalcLng by viewModel.lastRecalcLng.collectAsStateWithLifecycle()
    val innerRadiusR by viewModel.innerRadiusR.collectAsStateWithLifecycle()
    val outerRadiusN by viewModel.outerRadiusN.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isMapDarkTheme = isMapDark(mapThemeSetting, isSystemDark)

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessage.collect { uiText ->
            val msg = when (uiText) {
                is UiText.DynamicString -> uiText.value
                is UiText.StringResource -> context.applicationContext.getString(uiText.resId)
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

    // View switcher state
    var isMapView by rememberSaveable { mutableStateOf(false) }
    var selectedLocationOnMap by remember { mutableStateOf<LocationEntity?>(null) }
    var currentUserLocation by remember { mutableStateOf<android.location.Location?>(null) }

    LaunchedEffect(isMapView) {
        if (isMapView) {
            currentUserLocation = viewModel.getCurrentLocation()
        }
    }

    val showEmptyState = if (isMapView) {
        activeReminders.isEmpty()
    } else {
        reminders.isEmpty()
    }

    // Dialog State
    var showDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var selectedLocationId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var transitionType by remember { mutableIntStateOf(Geofence.GEOFENCE_TRANSITION_ENTER) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val formState = ReminderFormState(
        editingReminder = editingReminder,
        selectedLocationId = selectedLocationId,
        message = message,
        transitionType = transitionType,
        dropdownExpanded = dropdownExpanded
    )

    val formActions = ReminderFormActions(
        onLocationSelected = {
            selectedLocationId = it
            dropdownExpanded = false
        },
        onMessageChange = { message = it },
        onTransitionTypeChange = { transitionType = it },
        onDropdownExpandedChange = { dropdownExpanded = it },
        onSave = {
            saveReminder(viewModel, editingReminder, selectedLocationId, message, transitionType)
            showDialog = false
            editingReminder = null
        },
        onDelete = {
            val editing = editingReminder
            if (editing != null) {
                viewModel.cancelReminder(editing.id)
                showDialog = false
                editingReminder = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.applicationContext.getString(R.string.toast_reminder_deleted),
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

    val contentState = RemindersContentState(
        isMapView = isMapView,
        showEmptyState = showEmptyState,
        reminders = reminders,
        activeReminders = activeReminders,
        completedReminders = completedReminders,
        locationAliasMap = locationAliasMap,
        selectedLocationOnMap = selectedLocationOnMap,
        mapViewData = ReminderMapViewData(
            reminders = activeReminders,
            locations = locations,
            selectedLocation = selectedLocationOnMap,
            spatialArea = SpatialRecalculationArea(
                latitude = lastRecalcLat,
                longitude = lastRecalcLng,
                innerRadiusMeters = innerRadiusR * 1000f,
                outerRadiusMeters = outerRadiusN * 1000f
            ),
            currentUserLocation = currentUserLocation
        ),
        isMapDarkTheme = isMapDarkTheme
    )

    val contentActions = RemindersContentActions(
        onListSelected = { isMapView = false },
        onMapSelected = { isMapView = true },
        onCancelActive = { reminder ->
            viewModel.cancelReminder(reminder.id)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.applicationContext.getString(R.string.toast_reminder_cancelled),
                    duration = SnackbarDuration.Short
                )
            }
        },
        onDeleteCompleted = { reminder ->
            viewModel.cancelReminder(reminder.id)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.applicationContext.getString(R.string.toast_reminder_deleted),
                    duration = SnackbarDuration.Short
                )
            }
        },
        onEditReminder = { reminder ->
            editingReminder = reminder
            selectedLocationId = reminder.locationId
            message = reminder.message
            transitionType = reminder.transitionType
            showDialog = true
        },
        onDeleteReminderOnMap = { reminder ->
            viewModel.cancelReminder(reminder.id)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.applicationContext.getString(R.string.toast_reminder_deleted),
                    duration = SnackbarDuration.Short
                )
            }
            val remaining = activeReminders.filter { it.locationId == selectedLocationOnMap?.id && it.id != reminder.id }
            if (remaining.isEmpty()) {
                selectedLocationOnMap = null
            }
        },
        onLocationSelected = { selectedLocationOnMap = it },
        onDismissSelectedLocation = { selectedLocationOnMap = null },
        onAddReminder = {
            editingReminder = null
            selectedLocationId = locations.firstOrNull()?.id ?: ""
            message = ""
            transitionType = Geofence.GEOFENCE_TRANSITION_ENTER
            showDialog = true
        }
    )

    Column(modifier = modifier.fillMaxSize()) {
        BackgroundLocationWarningBanner()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        ) {
            RemindersMainContent(
                state = contentState,
                actions = contentActions,
                modifier = Modifier.fillMaxSize()
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        ReminderBottomSheet(
            showDialog = showDialog,
            sheetState = sheetState,
            formState = formState,
            formActions = formActions,
            locations = locations,
            onDismissRequest = {
                showDialog = false
                editingReminder = null
            }
        )
    }
}

private data class RemindersContentState(
    val isMapView: Boolean,
    val showEmptyState: Boolean,
    val reminders: List<ReminderEntity>,
    val activeReminders: List<ReminderEntity>,
    val completedReminders: List<ReminderEntity>,
    val locationAliasMap: Map<String, String>,
    val selectedLocationOnMap: LocationEntity?,
    val mapViewData: ReminderMapViewData,
    val isMapDarkTheme: Boolean
)

private data class RemindersContentActions(
    val onListSelected: () -> Unit,
    val onMapSelected: () -> Unit,
    val onCancelActive: (ReminderEntity) -> Unit,
    val onDeleteCompleted: (ReminderEntity) -> Unit,
    val onEditReminder: (ReminderEntity) -> Unit,
    val onDeleteReminderOnMap: (ReminderEntity) -> Unit,
    val onLocationSelected: (LocationEntity?) -> Unit,
    val onDismissSelectedLocation: () -> Unit,
    val onAddReminder: () -> Unit
)

@Composable
private fun RemindersMainContent(
    state: RemindersContentState,
    actions: RemindersContentActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        androidx.compose.animation.AnimatedVisibility(
            visible = state.showEmptyState,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val suggestionRes = if (android.os.Build.VERSION.SDK_INT >= 36) {
                R.string.empty_reminders_suggestion
            } else {
                R.string.empty_reminders_suggestion_no_gemini
            }
            EmptyState(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.empty_reminders_title),
                suggestion = stringResource(suggestionRes)
            )
        }

        // List View
        androidx.compose.animation.AnimatedVisibility(
            visible = !state.isMapView && state.reminders.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RemindersListContent(
                activeReminders = state.activeReminders,
                completedReminders = state.completedReminders,
                locationAliasMap = state.locationAliasMap,
                onCancelActive = actions.onCancelActive,
                onDeleteCompleted = actions.onDeleteCompleted,
                onReminderClick = actions.onEditReminder
            )
        }

        // Map View
        androidx.compose.animation.AnimatedVisibility(
            visible = state.isMapView && state.activeReminders.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ReminderMapView(
                    data = state.mapViewData,
                    onLocationSelected = actions.onLocationSelected,
                    isDarkTheme = state.isMapDarkTheme
                )

                if (state.selectedLocationOnMap != null) {
                    SelectedReminderLocationCard(
                        location = state.selectedLocationOnMap,
                        reminders = state.activeReminders.filter { it.locationId == state.selectedLocationOnMap.id },
                        onEdit = actions.onEditReminder,
                        onDelete = actions.onDeleteReminderOnMap,
                        onDismiss = actions.onDismissSelectedLocation,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }

        if (state.selectedLocationOnMap == null) {
            FloatingActionButton(
                onClick = actions.onAddReminder,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_description_add_reminder))
            }
        }

        ViewModeSwitcher(
            isMapView = state.isMapView,
            onListSelected = actions.onListSelected,
            onMapSelected = actions.onMapSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderBottomSheet(
    showDialog: Boolean,
    sheetState: SheetState,
    formState: ReminderFormState,
    formActions: ReminderFormActions,
    locations: List<LocationEntity>,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            ReminderBottomSheetContent(
                state = formState,
                actions = formActions,
                locations = locations
            )
        }
    }
}

private data class ReminderFormState(
    val editingReminder: ReminderEntity?,
    val selectedLocationId: String,
    val message: String,
    val transitionType: Int,
    val dropdownExpanded: Boolean
)

private data class ReminderFormActions(
    val onLocationSelected: (String) -> Unit,
    val onMessageChange: (String) -> Unit,
    val onTransitionTypeChange: (Int) -> Unit,
    val onDropdownExpandedChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onCancel: () -> Unit
)

@Composable
private fun RemindersListContent(
    activeReminders: List<ReminderEntity>,
    completedReminders: List<ReminderEntity>,
    locationAliasMap: Map<String, String>,
    onCancelActive: (ReminderEntity) -> Unit,
    onDeleteCompleted: (ReminderEntity) -> Unit,
    onReminderClick: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 64.dp, bottom = 16.dp)
    ) {
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
                    onCancel = { onCancelActive(reminder) },
                    onClick = { onReminderClick(reminder) },
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
                    onDelete = { onDeleteCompleted(reminder) },
                    modifier = Modifier
                        .alpha(0.6f)
                        .animateItem()
                ) {
                    Box(
                        modifier = Modifier.clickable { onReminderClick(reminder) }
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

@Composable
private fun ReminderBottomSheetContent(
    state: ReminderFormState,
    actions: ReminderFormActions,
    locations: List<LocationEntity>,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (state.editingReminder == null) {
                stringResource(R.string.dialog_new_reminder)
            } else {
                stringResource(R.string.dialog_edit_reminder)
            },
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
            ReminderLocationSelector(
                locations = locations,
                selectedLocationId = state.selectedLocationId,
                dropdownExpanded = state.dropdownExpanded,
                onDropdownExpandedChange = actions.onDropdownExpandedChange,
                onLocationSelected = actions.onLocationSelected
            )

            OutlinedTextField(
                value = state.message,
                onValueChange = actions.onMessageChange,
                label = { Text(stringResource(R.string.reminder_message)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            TransitionTypeSelector(
                transitionType = state.transitionType,
                onTransitionTypeChange = actions.onTransitionTypeChange
            )
        }

        val isValid = locations.isNotEmpty() && state.message.isNotBlank() && state.selectedLocationId.isNotEmpty()

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialogDismissButtons(
                isEditing = state.editingReminder != null,
                onDelete = actions.onDelete,
                onCancel = actions.onCancel
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = actions.onSave,
                enabled = isValid,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }
}

@Composable
private fun ReminderLocationSelector(
    locations: List<LocationEntity>,
    selectedLocationId: String,
    dropdownExpanded: Boolean,
    onDropdownExpandedChange: (Boolean) -> Unit,
    onLocationSelected: (String) -> Unit
) {
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
                .clickable { onDropdownExpandedChange(true) }
        )
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { onDropdownExpandedChange(false) },
            modifier = Modifier.fillMaxWidth()
        ) {
            locations.forEach { location ->
                DropdownMenuItem(
                    text = { Text(location.alias) },
                    onClick = {
                        onLocationSelected(location.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TransitionOptionButton(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        color = containerColor,
        modifier = Modifier.weight(1f)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}

@Composable
private fun TransitionTypeSelector(
    transitionType: Int,
    onTransitionTypeChange: (Int) -> Unit
) {
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
            TransitionOptionButton(
                selected = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER,
                icon = Icons.Filled.LocationOn,
                label = stringResource(R.string.label_arrival),
                onClick = { onTransitionTypeChange(Geofence.GEOFENCE_TRANSITION_ENTER) }
            )
            TransitionOptionButton(
                selected = transitionType == Geofence.GEOFENCE_TRANSITION_EXIT,
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = stringResource(R.string.label_departure),
                onClick = { onTransitionTypeChange(Geofence.GEOFENCE_TRANSITION_EXIT) }
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

@Composable
private fun SelectedReminderLocationCard(
    location: LocationEntity,
    reminders: List<ReminderEntity>,
    onEdit: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = location.alias,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.content_description_dismiss),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (reminders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_active_reminders_for_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminders.forEachIndexed { index, reminder ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        SelectedReminderItemRow(
                            reminder = reminder,
                            onEdit = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedReminderItemRow(
    reminder: ReminderEntity,
    onEdit: (ReminderEntity) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.SuggestionChip(
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
                    modifier = Modifier.height(24.dp)
                )

                if (reminder.isInRange) {
                    androidx.compose.material3.SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = stringResource(R.string.label_in_range),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEdit(reminder) }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.content_description_edit_reminder),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = { onDelete(reminder) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.content_description_delete_reminder),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
