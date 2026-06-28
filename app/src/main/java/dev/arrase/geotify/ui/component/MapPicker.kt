package dev.arrase.geotify.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import dev.arrase.geotify.R
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPicker(
    initialLatitude: Double?,
    initialLongitude: Double?,
    radiusMeters: Float,
    onGetCurrentLocation: suspend () -> android.location.Location?,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Configure osmdroid cache to avoid external storage permission issues
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    var selectedPoint by remember {
        mutableStateOf(
            if (initialLatitude != null && initialLongitude != null) {
                GeoPoint(initialLatitude, initialLongitude)
            } else {
                // Default coordinates (e.g. Madrid center) if no location is available
                GeoPoint(40.416775, -3.703790)
            }
        )
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Center on selectedPoint initially and query GPS if no initial location is provided
    LaunchedEffect(mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (initialLatitude != null && initialLongitude != null) {
            map.controller.setCenter(selectedPoint)
        } else {
            // Set initial center to Madrid while loading current GPS position
            map.controller.setCenter(selectedPoint)
            val loc = onGetCurrentLocation()
            if (loc != null) {
                val point = GeoPoint(loc.latitude, loc.longitude)
                selectedPoint = point
                map.controller.animateTo(point)
            }
        }
    }

    // Lifecycle management to avoid memory and thread leaks
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(mapViewRef, lifecycle) {
        val map = mapViewRef ?: return@DisposableEffect onDispose {}
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> map.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onDetach()
        }
    }

    // Cache the custom marker icon
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val markerIcon = remember(context, primaryColor) {
        getTintedMarkerIcon(context, primaryColor, sizeDp = 44)
    }

    // Colors for circle from Theme
    val circleFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f).toArgb()
    val circleStrokeColor = MaterialTheme.colorScheme.primary.toArgb()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(16.0)
                            mapViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        // Apply dark mode styling to map tiles
                        if (isDarkTheme) {
                            val filter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                                -0.1491f, -0.5005f, -0.0504f, 0f, 215f,
                                -0.1491f, -0.5005f, -0.0504f, 0f, 215f,
                                -0.1491f, -0.5005f, -0.0504f, 0f, 230f,
                                0f,        0f,        0f,        1f, 0f
                            )))
                            map.overlayManager.tilesOverlay.setColorFilter(filter)
                        } else {
                            map.overlayManager.tilesOverlay.setColorFilter(null)
                        }

                        map.overlays.clear()

                        // Register MapEventsOverlay to handle taps on the map
                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                selectedPoint = p
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean {
                                selectedPoint = p
                                return true
                            }
                        })
                        map.overlays.add(mapEventsOverlay)

                        // Circle overlay representing the geofence radius
                        val circle = Polygon().apply {
                            points = Polygon.pointsAsCircle(selectedPoint, radiusMeters.toDouble())
                            fillPaint.color = circleFillColor
                            outlinePaint.color = circleStrokeColor
                            outlinePaint.strokeWidth = 3f
                        }
                        map.overlays.add(circle)

                        // Marker representing the currently selected point
                        val marker = Marker(map).apply {
                            position = selectedPoint
                            title = context.applicationContext.getString(R.string.label_selected_location)
                            icon = markerIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        map.overlays.add(marker)

                        map.invalidate()
                    }
                )

                // Floating Top App Bar for Map Picker
                TopAppBar(
                    title = { Text(stringResource(R.string.label_map_picker_title), style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                )

                // Floating My Location Button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val loc = onGetCurrentLocation()
                            if (loc != null) {
                                val point = GeoPoint(loc.latitude, loc.longitude)
                                selectedPoint = point
                                mapViewRef?.controller?.animateTo(point)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 196.dp, end = 16.dp)
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.content_description_center_on_gps))
                }

                // Floating details card at the bottom
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_selected_location),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.label_latitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%.6f", selectedPoint.latitude),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.label_longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(Locale.US, "%.6f", selectedPoint.longitude),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.label_geofence_radius),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.label_meters, radiusMeters.toInt()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onLocationSelected(selectedPoint.latitude, selectedPoint.longitude)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_confirm_selection))
                        }
                    }
                }
            }
        }
    }
}

private fun getTintedMarkerIcon(context: Context, color: Int, sizeDp: Int = 38): Drawable {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location)
        ?: return ColorDrawable(color)
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    val mutated = drawable.mutate()
    DrawableCompat.setTint(mutated, color)
    mutated.draw(canvas)
    return BitmapDrawable(context.resources, bitmap)
}
