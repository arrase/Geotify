package dev.arrase.geotify.ui.screen

import android.location.Location
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.arrase.geotify.R
import dev.arrase.geotify.data.ThemeSetting
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.ui.UiText
import dev.arrase.geotify.ui.component.BackgroundLocationWarningBanner
import dev.arrase.geotify.ui.component.DialogDismissButtons
import dev.arrase.geotify.ui.component.EmptyState
import dev.arrase.geotify.ui.component.LocationMapView
import dev.arrase.geotify.ui.component.LocationRow
import dev.arrase.geotify.ui.component.MapPicker
import dev.arrase.geotify.ui.component.SwipeToDeleteContainer
import dev.arrase.geotify.ui.component.ViewModeSwitcher
import kotlinx.coroutines.launch
import java.util.Locale

private fun isMapDark(themeSetting: ThemeSetting, isSystemDark: Boolean): Boolean = when (themeSetting) {
    ThemeSetting.SYSTEM -> isSystemDark
    ThemeSetting.LIGHT -> false
    ThemeSetting.DARK -> true
}

private fun isValidCoordinate(coord: Double?, min: Double, max: Double): Boolean =
    coord != null && coord in min..max

private fun toCoordinatesPair(lat: Double?, lng: Double?): Pair<Double, Double>? =
    if (lat != null && lng != null) Pair(lat, lng) else null

private fun saveLocation(
    viewModel: LocationsViewModel,
    editingLocation: LocationEntity?,
    alias: String,
    lat: Double?,
    lng: Double?,
    radiusMeters: Float,
    responsivenessMinutes: Float
) {
    if (lat == null || lng == null) return
    val responsivenessMs = (responsivenessMinutes * 60000).toInt()
    if (editingLocation == null) {
        viewModel.saveLocation(alias, lat, lng, radiusMeters, responsivenessMs)
    } else {
        viewModel.updateLocation(
            editingLocation.copy(
                alias = alias,
                latitude = lat,
                longitude = lng,
                radiusMeters = radiusMeters,
                notificationResponsivenessMs = responsivenessMs
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    viewModel: LocationsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapThemeSetting by viewModel.mapTheme.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isMapDarkTheme = isMapDark(mapThemeSetting, isSystemDark)
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

    var isMapView by rememberSaveable { mutableStateOf(false) }
    var selectedLocationOnMap by remember { mutableStateOf<LocationEntity?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }

    val aliasExists = remember(alias, editingLocation, locations) {
        locations.any { it.alias.equals(alias, ignoreCase = true) && it.id != editingLocation?.id }
    }

    val lat = latitudeString.toDoubleOrNull()
    val lng = longitudeString.toDoubleOrNull()
    val isLatitudeValid = isValidCoordinate(lat, -90.0, 90.0)
    val isLongitudeValid = isValidCoordinate(lng, -180.0, 180.0)

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

    val formState = LocationFormState(
        alias = alias,
        aliasExists = aliasExists,
        latitudeString = latitudeString,
        isLatitudeValid = isLatitudeValid,
        longitudeString = longitudeString,
        isLongitudeValid = isLongitudeValid,
        radiusMeters = radiusMeters,
        responsivenessMinutes = responsivenessMinutes,
        isEditing = editingLocation != null,
        isGpsLoading = isGpsLoading,
        isValid = alias.isNotBlank() && isLatitudeValid && isLongitudeValid && !aliasExists,
        showResponsivenessInfo = showResponsivenessInfo
    )

    val formActions = LocationFormActions(
        onAliasChange = { alias = it },
        onLatitudeChange = { latitudeString = it },
        onLongitudeChange = { longitudeString = it },
        onRadiusChange = { radiusMeters = it },
        onResponsivenessChange = { responsivenessMinutes = it },
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
            saveLocation(viewModel, editingLocation, alias, lat, lng, radiusMeters, responsivenessMinutes)
            showDialog = false
            editingLocation = null
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
        onResponsivenessInfoChange = { showResponsivenessInfo = it }
    )

    val mainContentState = LocationsContentState(
        isMapView = isMapView,
        locations = locations,
        activeReminderCounts = activeReminderCounts,
        selectedLocationOnMap = selectedLocationOnMap,
        isMapDarkTheme = isMapDarkTheme
    )

    val mainContentActions = LocationsContentActions(
        onListSelected = { isMapView = false },
        onMapSelected = { isMapView = true },
        onLocationSelected = { selectedLocationOnMap = it },
        onDeleteLocation = { location ->
            viewModel.deleteLocation(location.alias)
            if (selectedLocationOnMap?.id == location.id) {
                selectedLocationOnMap = null
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.applicationContext.getString(R.string.toast_location_deleted, location.alias),
                    duration = SnackbarDuration.Short
                )
            }
        },
        onEditLocation = { location -> openFormForEditing(location) },
        onDismissSelectedLocation = { selectedLocationOnMap = null },
        onAddLocation = { openFormForNew() }
    )

    Column(modifier = modifier.fillMaxSize()) {
        BackgroundLocationWarningBanner()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
        ) {
            LocationsMainContent(
                state = mainContentState,
                actions = mainContentActions,
                modifier = Modifier.fillMaxSize()
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            LocationMapPickerModal(
                show = showMapPicker,
                initialCoordinates = toCoordinatesPair(lat, lng),
                radiusMeters = radiusMeters,
                isDarkTheme = isMapDarkTheme,
                onGetCurrentLocation = { viewModel.getCurrentLocation() },
                onLocationSelected = { selectedLat, selectedLng ->
                    latitudeString = String.format(Locale.US, "%.6f", selectedLat)
                    longitudeString = String.format(Locale.US, "%.6f", selectedLng)
                    showMapPicker = false
                    showDialog = true
                },
                onDismiss = {
                    showMapPicker = false
                    showDialog = true
                }
            )
        }

        LocationBottomSheet(
            showDialog = showDialog,
            sheetState = sheetState,
            formState = formState,
            formActions = formActions,
            onDismissRequest = {
                showDialog = false
                editingLocation = null
            }
        )
    }
}

@Composable
private fun LocationMapPickerModal(
    show: Boolean,
    initialCoordinates: Pair<Double, Double>?,
    radiusMeters: Float,
    isDarkTheme: Boolean,
    onGetCurrentLocation: suspend () -> Location?,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        MapPicker(
            initialCoordinates = initialCoordinates,
            radiusMeters = radiusMeters,
            onGetCurrentLocation = onGetCurrentLocation,
            onLocationSelected = onLocationSelected,
            onDismiss = onDismiss,
            isDarkTheme = isDarkTheme
        )
    }
}

private data class LocationsContentState(
    val isMapView: Boolean,
    val locations: List<LocationEntity>,
    val activeReminderCounts: Map<String, Int>,
    val selectedLocationOnMap: LocationEntity?,
    val isMapDarkTheme: Boolean
)

private data class LocationsContentActions(
    val onListSelected: () -> Unit,
    val onMapSelected: () -> Unit,
    val onLocationSelected: (LocationEntity?) -> Unit,
    val onDeleteLocation: (LocationEntity) -> Unit,
    val onEditLocation: (LocationEntity) -> Unit,
    val onDismissSelectedLocation: () -> Unit,
    val onAddLocation: () -> Unit
)

@Composable
private fun LocationsMainContent(
    state: LocationsContentState,
    actions: LocationsContentActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        androidx.compose.animation.AnimatedVisibility(
            visible = !state.isMapView && state.locations.isEmpty(),
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

        // List View
        androidx.compose.animation.AnimatedVisibility(
            visible = !state.isMapView && state.locations.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LocationsListContent(
                locations = state.locations,
                activeReminderCounts = state.activeReminderCounts,
                onDeleteLocation = actions.onDeleteLocation,
                onEditLocation = actions.onEditLocation
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = state.isMapView,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LocationMapView(
                    locations = state.locations,
                    selectedLocation = state.selectedLocationOnMap,
                    onLocationSelected = actions.onLocationSelected,
                    isDarkTheme = state.isMapDarkTheme
                )

                if (state.selectedLocationOnMap != null) {
                    SelectedLocationCard(
                        location = state.selectedLocationOnMap,
                        onEdit = { actions.onEditLocation(state.selectedLocationOnMap) },
                        onDelete = { actions.onDeleteLocation(state.selectedLocationOnMap) },
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
                onClick = actions.onAddLocation,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_description_add_location))
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

// ── Extracted Composables ──

data class LocationFormState(
    val alias: String,
    val aliasExists: Boolean,
    val latitudeString: String,
    val isLatitudeValid: Boolean,
    val longitudeString: String,
    val isLongitudeValid: Boolean,
    val radiusMeters: Float,
    val responsivenessMinutes: Float,
    val isEditing: Boolean,
    val isGpsLoading: Boolean,
    val isValid: Boolean,
    val showResponsivenessInfo: Boolean
)

data class LocationFormActions(
    val onAliasChange: (String) -> Unit,
    val onLatitudeChange: (String) -> Unit,
    val onLongitudeChange: (String) -> Unit,
    val onRadiusChange: (Float) -> Unit,
    val onResponsivenessChange: (Float) -> Unit,
    val onUseGps: () -> Unit,
    val onPickFromMap: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: (() -> Unit)?,
    val onCancel: () -> Unit,
    val onResponsivenessInfoChange: (Boolean) -> Unit
)

@Composable
private fun LocationsListContent(
    locations: List<LocationEntity>,
    activeReminderCounts: Map<String, Int>,
    onDeleteLocation: (LocationEntity) -> Unit,
    onEditLocation: (LocationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 64.dp, bottom = 16.dp)
    ) {
        items(
            items = locations,
            key = { it.id }
        ) { location ->
            SwipeToDeleteContainer(
                onDelete = { onDeleteLocation(location) },
                modifier = Modifier.animateItem()
            ) {
                Box(
                    modifier = Modifier.clickable { onEditLocation(location) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationBottomSheet(
    showDialog: Boolean,
    sheetState: SheetState,
    formState: LocationFormState,
    formActions: LocationFormActions,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState
        ) {
            LocationFormContent(
                state = formState,
                actions = formActions
            )
        }
    }
}

@Composable
private fun LocationFormContent(
    state: LocationFormState,
    actions: LocationFormActions,
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
            text = if (!state.isEditing) stringResource(R.string.dialog_new_location) else stringResource(R.string.dialog_edit_location),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LocationCoordinatesInputs(state = state, actions = actions)

        LocationGpsButtons(
            isGpsLoading = state.isGpsLoading,
            onUseGps = actions.onUseGps,
            onPickFromMap = actions.onPickFromMap
        )

        RadiusSlider(
            value = state.radiusMeters,
            onValueChange = actions.onRadiusChange
        )

        ResponsivenessSlider(
            value = state.responsivenessMinutes,
            onValueChange = actions.onResponsivenessChange,
            onInfoClick = { actions.onResponsivenessInfoChange(true) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LocationFormButtons(
            isEditing = state.isEditing,
            isValid = state.isValid,
            onDelete = actions.onDelete,
            onCancel = actions.onCancel,
            onSave = actions.onSave
        )
    }

    if (state.showResponsivenessInfo) {
        ResponsivenessInfoDialog(onDismiss = { actions.onResponsivenessInfoChange(false) })
    }
}

@Composable
private fun LocationCoordinatesInputs(
    state: LocationFormState,
    actions: LocationFormActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = state.alias,
            onValueChange = actions.onAliasChange,
            label = { Text(stringResource(R.string.alias_hint)) },
            singleLine = true,
            isError = state.aliasExists,
            supportingText = {
                if (state.aliasExists) {
                    Text(stringResource(R.string.err_alias_exists), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.latitudeString,
            onValueChange = actions.onLatitudeChange,
            label = { Text(stringResource(R.string.lat_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.latitudeString.isNotEmpty() && !state.isLatitudeValid,
            supportingText = {
                if (state.latitudeString.isNotEmpty() && !state.isLatitudeValid) {
                    Text(stringResource(R.string.err_lat_invalid), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.longitudeString,
            onValueChange = actions.onLongitudeChange,
            label = { Text(stringResource(R.string.lng_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = state.longitudeString.isNotEmpty() && !state.isLongitudeValid,
            supportingText = {
                if (state.longitudeString.isNotEmpty() && !state.isLongitudeValid) {
                    Text(stringResource(R.string.err_lng_invalid), color = MaterialTheme.colorScheme.error)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LocationGpsButtons(
    isGpsLoading: Boolean,
    onUseGps: () -> Unit,
    onPickFromMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
}

@Composable
private fun LocationFormButtons(
    isEditing: Boolean,
    isValid: Boolean,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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

@Composable
private fun ResponsivenessInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.btn_ok))
            }
        }
    )
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
