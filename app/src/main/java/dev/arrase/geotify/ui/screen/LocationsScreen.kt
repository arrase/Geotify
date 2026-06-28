package dev.arrase.geotify.ui.screen

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import dev.arrase.geotify.ui.UiText
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.R
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.ui.component.BackgroundLocationWarningBanner
import dev.arrase.geotify.ui.component.DialogDismissButtons
import dev.arrase.geotify.ui.component.EmptyState
import dev.arrase.geotify.ui.component.LocationMapView
import dev.arrase.geotify.ui.component.LocationRow
import dev.arrase.geotify.ui.component.MapPicker
import dev.arrase.geotify.ui.component.SwipeToDeleteContainer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    viewModel: LocationsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapThemeSetting by viewModel.mapTheme.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isMapDarkTheme = when (mapThemeSetting) {
        ThemeSetting.SYSTEM -> isSystemDark
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
    }
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val activeReminderCounts by viewModel.activeReminderCounts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var alias by remember { mutableStateOf("") }
    var latitudeString by remember { mutableStateOf("") }
    var longitudeString by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableFloatStateOf(150f) }
    var responsivenessMinutes by remember { mutableFloatStateOf(0f) }
    var showResponsivenessInfo by remember { mutableStateOf(false) }
    var isGpsLoading by remember { mutableStateOf(false) }

    // Map view and Map picker states
    var isMapView by rememberSaveable { mutableStateOf(false) }
    var selectedLocationOnMap by remember { mutableStateOf<LocationEntity?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }

    val aliasExists = remember(alias, editingLocation, locations) {
        locations.any { it.alias.equals(alias, ignoreCase = true) && it.id != editingLocation?.id }
    }

    val lat = latitudeString.toDoubleOrNull()
    val lng = longitudeString.toDoubleOrNull()
    val isLatitudeValid = lat != null && lat in -90.0..90.0
    val isLongitudeValid = lng != null && lng in -180.0..180.0

    fun openFormForNew() {
        editingLocation = null
        alias = ""
        latitudeString = ""
        longitudeString = ""
        radiusMeters = 150f
        responsivenessMinutes = 0f
        showDialog = true
    }

    fun openFormForEditing(location: LocationEntity) {
        editingLocation = location
        alias = location.alias
        latitudeString = location.latitude.toString()
        longitudeString = location.longitude.toString()
        radiusMeters = location.radiusMeters
        responsivenessMinutes = location.notificationResponsivenessMs / 60000f
        showDialog = true
    }

    Column(modifier.fillMaxSize()) {
        BackgroundLocationWarningBanner()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isMapView && locations.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val suggestionRes = if (android.os.Build.VERSION.SDK_INT >= 36) {
                    R.string.empty_locations_suggestion
                } else {
                    R.string.empty_locations_suggestion_no_gemini
                }
                EmptyState(
                    icon = Icons.Filled.LocationOn,
                    title = stringResource(R.string.empty_locations_title),
                    suggestion = stringResource(suggestionRes)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !isMapView && locations.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 64.dp, bottom = 16.dp)
                ) {
                    items(
                        items = locations,
                        key = { it.id }
                    ) { location ->
                        SwipeToDeleteContainer(
                            onDelete = {
                                viewModel.deleteLocation(location.alias)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.applicationContext.getString(R.string.toast_location_deleted, location.alias),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier.animateItem()
                        ) {
                            Box(
                                modifier = Modifier.clickable { openFormForEditing(location) }
                            ) {
                                LocationRow(
                                    location = location,
                                    activeReminderCount = activeReminderCounts[location.id] ?: 0
                                )
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isMapView,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LocationMapView(
                        locations = locations,
                        selectedLocation = selectedLocationOnMap,
                        onLocationSelected = { selectedLocationOnMap = it },
                        isDarkTheme = isMapDarkTheme
                    )

                    if (selectedLocationOnMap != null) {
                        SelectedLocationCard(
                            location = selectedLocationOnMap!!,
                            onEdit = { openFormForEditing(selectedLocationOnMap!!) },
                            onDelete = {
                                viewModel.deleteLocation(selectedLocationOnMap!!.alias)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = context.applicationContext.getString(R.string.toast_location_deleted, selectedLocationOnMap!!.alias),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                selectedLocationOnMap = null
                            },
                            onDismiss = { selectedLocationOnMap = null },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
            }

            if (selectedLocationOnMap == null) {
                FloatingActionButton(
                    onClick = { openFormForNew() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_description_add_location))
                }
            }

            ViewModeSwitcher(
                isMapView = isMapView,
                onListSelected = { isMapView = false },
                onMapSelected = { isMapView = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )

        if (showDialog) {
            ModalBottomSheet(
                onDismissRequest = {
                    showDialog = false
                    editingLocation = null
                },
                sheetState = sheetState
            ) {
                LocationFormContent(
                    alias = alias,
                    onAliasChange = { alias = it },
                    aliasExists = aliasExists,
                    latitudeString = latitudeString,
                    onLatitudeChange = { latitudeString = it },
                    isLatitudeValid = isLatitudeValid,
                    longitudeString = longitudeString,
                    onLongitudeChange = { longitudeString = it },
                    isLongitudeValid = isLongitudeValid,
                    radiusMeters = radiusMeters,
                    onRadiusChange = { radiusMeters = it },
                    responsivenessMinutes = responsivenessMinutes,
                    onResponsivenessChange = { responsivenessMinutes = it },
                    isEditing = editingLocation != null,
                    isGpsLoading = isGpsLoading,
                    onUseGps = {
                        isGpsLoading = true
                        scope.launch {
                            val loc = viewModel.getCurrentLocation()
                            if (loc != null) {
                                latitudeString = String.format(Locale.US, "%.6f", loc.latitude)
                                longitudeString = String.format(Locale.US, "%.6f", loc.longitude)
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = context.applicationContext.getString(R.string.err_gps_failed),
                                    duration = SnackbarDuration.Short
                                )
                            }
                            isGpsLoading = false
                        }
                    },
                    onPickFromMap = {
                        showMapPicker = true
                        showDialog = false
                    },
                    onSave = {
                        if (lat != null && lng != null) {
                            val currentEditing = editingLocation
                            val responsivenessMs = (responsivenessMinutes * 60000).toInt()
                            if (currentEditing == null) {
                                viewModel.saveLocation(alias, lat, lng, radiusMeters, responsivenessMs)
                            } else {
                                viewModel.updateLocation(
                                    currentEditing.copy(
                                        alias = alias,
                                        latitude = lat,
                                        longitude = lng,
                                        radiusMeters = radiusMeters,
                                        notificationResponsivenessMs = responsivenessMs
                                    )
                                )
                            }
                            showDialog = false
                            editingLocation = null
                        }
                    },
                    onDelete = if (editingLocation != null) {
                        {
                            viewModel.deleteLocation(editingLocation!!.alias)
                            showDialog = false
                            editingLocation = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.applicationContext.getString(R.string.toast_location_deleted, alias),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    } else null,
                    onCancel = {
                        showDialog = false
                        editingLocation = null
                    },
                    isValid = alias.isNotBlank() && isLatitudeValid && isLongitudeValid && !aliasExists,
                    showResponsivenessInfo = showResponsivenessInfo,
                    onResponsivenessInfoChange = { showResponsivenessInfo = it }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showMapPicker) {
            MapPicker(
                initialLatitude = latitudeString.toDoubleOrNull(),
                initialLongitude = longitudeString.toDoubleOrNull(),
                radiusMeters = radiusMeters,
                onGetCurrentLocation = { viewModel.getCurrentLocation() },
                onLocationSelected = { lat, lng ->
                    latitudeString = String.format(Locale.US, "%.6f", lat)
                    longitudeString = String.format(Locale.US, "%.6f", lng)
                    showMapPicker = false
                    showDialog = true
                },
                onDismiss = {
                    showMapPicker = false
                    showDialog = true
                },
                isDarkTheme = isMapDarkTheme
            )
        }
    }
}
}

// ── Extracted Composables ──

@Composable
private fun LocationFormContent(
    alias: String,
    onAliasChange: (String) -> Unit,
    aliasExists: Boolean,
    latitudeString: String,
    onLatitudeChange: (String) -> Unit,
    isLatitudeValid: Boolean,
    longitudeString: String,
    onLongitudeChange: (String) -> Unit,
    isLongitudeValid: Boolean,
    radiusMeters: Float,
    onRadiusChange: (Float) -> Unit,
    responsivenessMinutes: Float,
    onResponsivenessChange: (Float) -> Unit,
    isEditing: Boolean,
    isGpsLoading: Boolean,
    onUseGps: () -> Unit,
    onPickFromMap: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    isValid: Boolean,
    showResponsivenessInfo: Boolean,
    onResponsivenessInfoChange: (Boolean) -> Unit
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
            text = if (!isEditing) stringResource(R.string.dialog_new_location) else stringResource(R.string.dialog_edit_location),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = alias,
            onValueChange = onAliasChange,
            label = { Text(stringResource(R.string.alias_hint)) },
            singleLine = true,
            isError = aliasExists,
            supportingText = {
                if (aliasExists) {
                    Text(stringResource(R.string.err_alias_exists), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = latitudeString,
            onValueChange = onLatitudeChange,
            label = { Text(stringResource(R.string.lat_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = latitudeString.isNotEmpty() && !isLatitudeValid,
            supportingText = {
                if (latitudeString.isNotEmpty() && !isLatitudeValid) {
                    Text(stringResource(R.string.err_lat_invalid), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = longitudeString,
            onValueChange = onLongitudeChange,
            label = { Text(stringResource(R.string.lng_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = longitudeString.isNotEmpty() && !isLongitudeValid,
            supportingText = {
                if (longitudeString.isNotEmpty() && !isLongitudeValid) {
                    Text(stringResource(R.string.err_lng_invalid), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onUseGps,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                enabled = !isGpsLoading
            ) {
                if (isGpsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_querying_gps), maxLines = 1)
                } else {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_use_gps), maxLines = 1)
                }
            }

            Button(
                onClick = onPickFromMap,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.btn_select_on_map), maxLines = 1)
            }
        }

        RadiusSlider(
            value = radiusMeters,
            onValueChange = onRadiusChange
        )

        ResponsivenessSlider(
            value = responsivenessMinutes,
            onValueChange = onResponsivenessChange,
            onInfoClick = { onResponsivenessInfoChange(true) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialogDismissButtons(
                isEditing = isEditing,
                onDelete = { onDelete?.invoke() },
                onCancel = onCancel
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSave,
                enabled = isValid,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }

    if (showResponsivenessInfo) {
        AlertDialog(
            onDismissRequest = { onResponsivenessInfoChange(false) },
            title = {
                Text(
                    text = stringResource(R.string.label_notification_responsiveness),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.info_notification_responsiveness_desc)
                )
            },
            confirmButton = {
                Button(
                    onClick = { onResponsivenessInfoChange(false) }
                ) {
                    Text(stringResource(R.string.btn_ok))
                }
            }
        )
    }
}

@Composable
private fun RadiusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_geofence_radius),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.label_meters, value.toInt()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 50f..1000f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.label_50m), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(stringResource(R.string.label_1000m), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ResponsivenessSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_notification_responsiveness),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.content_description_info),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = if (value.toInt() == 0) {
                    stringResource(R.string.label_0min)
                } else {
                    stringResource(R.string.label_minutes_value, value.toInt())
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..10f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.label_0min), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text(stringResource(R.string.label_10min), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun SelectedLocationCard(
    location: LocationEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.alias,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.label_geofence_radius) + ": " + stringResource(R.string.label_meters, location.radiusMeters.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.label_notification_responsiveness) + ": " +
                            if (location.notificationResponsivenessMs == 0) {
                                stringResource(R.string.label_0min)
                            } else {
                                stringResource(R.string.label_minutes_value, location.notificationResponsivenessMs / 60000)
                            },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.label_latitude) + String.format(Locale.US, ": %.5f, ", location.latitude) +
                            stringResource(R.string.label_longitude) + String.format(Locale.US, ": %.5f", location.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.content_description_edit_location),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_description_delete_location),
                        tint = MaterialTheme.colorScheme.error
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
        }
    }
}

@Composable
private fun ViewModeSwitcher(
    isMapView: Boolean,
    onListSelected: () -> Unit,
    onMapSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        val containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        val selectedColor = MaterialTheme.colorScheme.primary
        val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        val onSelectedColor = MaterialTheme.colorScheme.onPrimary

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (!isMapView) selectedColor else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clickable(onClick = onListSelected)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.tab_list),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isMapView) onSelectedColor else contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isMapView) selectedColor else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .clickable(onClick = onMapSelected)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.tab_map),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isMapView) onSelectedColor else contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
