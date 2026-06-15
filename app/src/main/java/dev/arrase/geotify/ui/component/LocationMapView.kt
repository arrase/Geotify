package dev.arrase.geotify.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.LocationEntity
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

@Composable
fun LocationMapView(
    locations: List<LocationEntity>,
    selectedLocation: LocationEntity?,
    onLocationSelected: (LocationEntity?) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Configure osmdroid cache to avoid external storage permission issues
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(15.0)
        }
    }

    // Centering state
    var hasCentered by remember { mutableStateOf(false) }

    // Initial centering and zooming: fits bounds if multiple locations, centers on first if single
    LaunchedEffect(locations) {
        if (!hasCentered && locations.isNotEmpty()) {
            if (locations.size == 1) {
                val loc = locations.first()
                mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                mapView.controller.setZoom(15.0)
            } else {
                val points = locations.map { GeoPoint(it.latitude, it.longitude) }
                mapView.post {
                    try {
                        val box = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
                        mapView.zoomToBoundingBox(box, true, 120)
                    } catch (e: Exception) {
                        val loc = locations.first()
                        mapView.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                        mapView.controller.setZoom(15.0)
                    }
                }
            }
            hasCentered = true
        }
    }

    // Smoothly animate centering when selectedLocation changes
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let {
            mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
        }
    }

    // Lifecycle management to avoid memory and thread leaks
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(mapView, lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Pre-calculate/cache custom marker drawables when colors change
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()

    val selectedMarkerIcon = remember(context, primaryColor) {
        getTintedMarkerIcon(context, primaryColor, sizeDp = 44)
    }
    val defaultMarkerIcon = remember(context, secondaryColor) {
        getTintedMarkerIcon(context, secondaryColor, sizeDp = 36)
    }

    // Geofence Circle Colors from Theme
    val selectedFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f).toArgb()
    val selectedStrokeColor = MaterialTheme.colorScheme.primary.toArgb()

    val defaultFillColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f).toArgb()
    val defaultStrokeColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f).toArgb()

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
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

            // MapEventsOverlay to allow clicking on empty space to deselect
            val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    onLocationSelected(null)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    return false
                }
            })
            map.overlays.add(mapEventsOverlay)

            // Draw geofence radius circles and markers
            locations.forEach { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val isSelected = selectedLocation?.id == location.id

                // Circle overlay for geofence radius
                val circle = Polygon().apply {
                    points = Polygon.pointsAsCircle(geoPoint, location.radiusMeters.toDouble())
                    fillPaint.color = if (isSelected) selectedFillColor else defaultFillColor
                    outlinePaint.color = if (isSelected) selectedStrokeColor else defaultStrokeColor
                    outlinePaint.strokeWidth = if (isSelected) 5f else 3f
                }
                map.overlays.add(circle)

                // Custom Marker overlay
                val marker = Marker(map).apply {
                    position = geoPoint
                    title = location.alias
                    icon = if (isSelected) selectedMarkerIcon else defaultMarkerIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    setOnMarkerClickListener { _, _ ->
                        onLocationSelected(location)
                        true
                    }
                }
                map.overlays.add(marker)
            }

            map.invalidate()
        }
    )
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
