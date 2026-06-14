package dev.arrase.geotify.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.ui.GeotifyViewModel
import dev.arrase.geotify.ui.component.DialogDismissButtons
import dev.arrase.geotify.ui.component.EmptyState
import dev.arrase.geotify.ui.component.LocationRow
import dev.arrase.geotify.ui.component.SwipeToDeleteContainer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    viewModel: GeotifyViewModel,
    modifier: Modifier = Modifier
) {
    val locations by viewModel.locations.collectAsState()
    val activeReminderCounts by viewModel.activeReminderCounts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var alias by remember { mutableStateOf("") }
    var latitudeString by remember { mutableStateOf("") }
    var longitudeString by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableFloatStateOf(150f) }
    var isGpsLoading by remember { mutableStateOf(false) }

    val aliasExists = remember(alias, editingLocation, locations) {
        locations.any { it.alias.equals(alias, ignoreCase = true) && it.id != editingLocation?.id }
    }

    val lat = latitudeString.toDoubleOrNull()
    val lng = longitudeString.toDoubleOrNull()
    val isLatitudeValid = lat != null && lat in -90.0..90.0
    val isLongitudeValid = lng != null && lng in -180.0..180.0

    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = locations.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            EmptyState(
                icon = Icons.Filled.LocationOn,
                title = "No locations saved",
                suggestion = "Tap + to add a location, or tell Gemini: \"Save this location as home\""
            )
        }

        AnimatedVisibility(
            visible = locations.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    items = locations,
                    key = { it.id }
                ) { location ->
                    SwipeToDeleteContainer(
                        onDelete = {
                            viewModel.deleteLocation(location.alias)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "\"${location.alias}\" deleted",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        modifier = Modifier.animateItem()
                    ) {
                        Box(
                            modifier = Modifier.clickable {
                                editingLocation = location
                                alias = location.alias
                                latitudeString = location.latitude.toString()
                                longitudeString = location.longitude.toString()
                                radiusMeters = location.radiusMeters
                                showDialog = true
                            }
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

        FloatingActionButton(
            onClick = {
                editingLocation = null
                alias = ""
                latitudeString = ""
                longitudeString = ""
                radiusMeters = 150f
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Location")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    editingLocation = null
                },
                title = {
                    Text(
                        text = if (editingLocation == null) "New Location" else "Edit Location",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            label = { Text("Alias (e.g. Home, Office)") },
                            singleLine = true,
                            isError = aliasExists,
                            supportingText = {
                                if (aliasExists) {
                                    Text("Alias already exists", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = latitudeString,
                            onValueChange = { latitudeString = it },
                            label = { Text("Latitude ([-90.0, 90.0])") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = latitudeString.isNotEmpty() && !isLatitudeValid,
                            supportingText = {
                                if (latitudeString.isNotEmpty() && !isLatitudeValid) {
                                    Text("Must be between -90.0 and 90.0", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = longitudeString,
                            onValueChange = { longitudeString = it },
                            label = { Text("Longitude ([-180.0, 180.0])") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = longitudeString.isNotEmpty() && !isLongitudeValid,
                            supportingText = {
                                if (longitudeString.isNotEmpty() && !isLongitudeValid) {
                                    Text("Must be between -180.0 and 180.0", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                isGpsLoading = true
                                scope.launch {
                                    val loc = viewModel.getCurrentLocation()
                                    if (loc != null) {
                                        latitudeString = String.format(Locale.US, "%.6f", loc.latitude)
                                        longitudeString = String.format(Locale.US, "%.6f", loc.longitude)
                                    }
                                    isGpsLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGpsLoading
                        ) {
                            if (isGpsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Querying GPS...")
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Use Current GPS Coordinates")
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Geofence Radius",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${radiusMeters.toInt()} meters",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Slider(
                                value = radiusMeters,
                                onValueChange = { radiusMeters = it },
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
                                Text("50m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text("1000m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                },
                confirmButton = {
                    val isValid = alias.isNotBlank() && isLatitudeValid && isLongitudeValid && !aliasExists
                    Button(
                        onClick = {
                            if (lat != null && lng != null) {
                                val editing = editingLocation
                                if (editing == null) {
                                    viewModel.saveLocation(alias, lat, lng, radiusMeters)
                                } else {
                                    viewModel.updateLocation(
                                        editing.copy(
                                            alias = alias,
                                            latitude = lat,
                                            longitude = lng,
                                            radiusMeters = radiusMeters
                                        )
                                    )
                                }
                                showDialog = false
                                editingLocation = null
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    val editing = editingLocation
                    DialogDismissButtons(
                        isEditing = editing != null,
                        onDelete = {
                            if (editing != null) {
                                viewModel.deleteLocation(editing.alias)
                                showDialog = false
                                editingLocation = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "\"${alias}\" deleted",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        onCancel = {
                            showDialog = false
                            editingLocation = null
                        }
                    )
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
