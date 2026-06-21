package dev.arrase.geotify.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Location
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
import dev.arrase.geotify.data.entity.ReminderEntity
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
fun ReminderMapView(
    reminders: List<ReminderEntity>,
    locations: List<LocationEntity>,
    selectedLocation: LocationEntity?,
    onLocationSelected: (LocationEntity?) -> Unit,
    lastRecalcLat: Double?,
    lastRecalcLng: Double?,
    innerRadiusMeters: Float,
    currentUserLocation: Location?,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Configure osmdroid cache
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hasCentered by remember { mutableStateOf(false) }

    // Map reminders to unique locations they target
    val activeLocations = remember(reminders, locations) {
        val locationIds = reminders.map { it.locationId }.toSet()
        locations.filter { locationIds.contains(it.id) }
    }

    // Initial centering and zooming
    LaunchedEffect(activeLocations, lastRecalcLat, lastRecalcLng, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (!hasCentered) {
            val points = mutableListOf<GeoPoint>()
            activeLocations.forEach {
                points.add(GeoPoint(it.latitude, it.longitude))
            }
            if (lastRecalcLat != null && lastRecalcLng != null) {
                points.add(GeoPoint(lastRecalcLat, lastRecalcLng))
            }
            if (currentUserLocation != null) {
                points.add(GeoPoint(currentUserLocation.latitude, currentUserLocation.longitude))
            }

            if (points.isNotEmpty()) {
                map.post {
                    try {
                        if (points.size == 1) {
                            map.controller.setCenter(points.first())
                            map.controller.setZoom(15.0)
                        } else {
                            val box = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
                            map.zoomToBoundingBox(box, true, 120)
                        }
                    } catch (e: Exception) {
                        if (lastRecalcLat != null && lastRecalcLng != null) {
                            map.controller.setCenter(GeoPoint(lastRecalcLat, lastRecalcLng))
                            map.controller.setZoom(14.0)
                        }
                    }
                }
                hasCentered = true
            }
        }
    }

    // Center on selectedLocation changes
    LaunchedEffect(selectedLocation, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        selectedLocation?.let {
            map.controller.animateTo(GeoPoint(it.latitude, it.longitude))
        }
    }

    // Lifecycle observer
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(mapViewRef, lifecycle) {
        val map = mapViewRef ?: return@DisposableEffect onDispose {}
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> map.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> map.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> map.onDetach()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onPause()
        }
    }

    // Custom pins and geofence colors
    val activeColor = android.graphics.Color.parseColor("#FF1744") // Extremely high contrast Vibrant Red
    val inactiveColor = android.graphics.Color.parseColor("#3F51B5") // Vibrant Indigo/Blue for active but out-of-range reminders
    val windowColor = android.graphics.Color.BLACK // Always black as requested

    val activeMarkerIcon = remember(context) {
        getTintedMarkerIcon(context, activeColor, sizeDp = 38)
    }
    val inactiveMarkerIcon = remember(context) {
        getTintedMarkerIcon(context, inactiveColor, sizeDp = 38)
    }
    val userMarkerIcon = remember(context) {
        getTintedMarkerIcon(context, android.graphics.Color.parseColor("#2196F3"), sizeDp = 24)
    }

    val activeFillColor = android.graphics.Color.argb(55, 255, 23, 68) // Vibrant Rose wash
    val activeStrokeColor = activeColor

    val inactiveFillColor = android.graphics.Color.argb(35, 63, 81, 181) // Vibrant Indigo wash
    val inactiveStrokeColor = inactiveColor

    val windowFillColor = android.graphics.Color.argb(20, 0, 0, 0) // Subtle dark wash for the master geofence
    val windowStrokeColor = windowColor


    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(15.0)
                onResume()
                mapViewRef = this
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { map ->
            // Apply dark mode styling
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

            // Deselect single tap listener
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

            // 1. Draw sliding window area (Master Geofence)
            if (lastRecalcLat != null && lastRecalcLng != null) {
                val centerPoint = GeoPoint(lastRecalcLat, lastRecalcLng)
                val windowCircle = Polygon().apply {
                    points = Polygon.pointsAsCircle(centerPoint, innerRadiusMeters.toDouble())
                    fillPaint.color = windowFillColor
                    outlinePaint.color = windowStrokeColor
                    outlinePaint.strokeWidth = 14f // Thicker boundary line (very prominent black outline)
                }
                map.overlays.add(windowCircle)

                // Master geofence center marker
                val centerMarker = Marker(map).apply {
                    position = centerPoint
                    title = "Sliding Window Center"
                    icon = getTintedMarkerIcon(context, windowStrokeColor, sizeDp = 20)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(centerMarker)
            }

            // 2. Draw user current location
            if (currentUserLocation != null) {
                val userPoint = GeoPoint(currentUserLocation.latitude, currentUserLocation.longitude)
                val userMarker = Marker(map).apply {
                    position = userPoint
                    title = "My Location"
                    icon = userMarkerIcon
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(userMarker)
            }

            // 3. Draw geofences and markers for active locations
            activeLocations.forEach { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val isSelected = selectedLocation?.id == location.id
                
                // Determine if this location's reminders are active in GMS (isInRange)
                val hasActiveReminderInRange = reminders.any { it.locationId == location.id && it.isInRange }

                val circle = Polygon().apply {
                    points = Polygon.pointsAsCircle(geoPoint, location.radiusMeters.toDouble())
                    fillPaint.color = if (hasActiveReminderInRange) activeFillColor else inactiveFillColor
                    outlinePaint.color = if (hasActiveReminderInRange) activeStrokeColor else inactiveStrokeColor
                    outlinePaint.strokeWidth = if (isSelected) 8f else (if (hasActiveReminderInRange) 5f else 3f)
                }
                map.overlays.add(circle)

                val marker = Marker(map).apply {
                    position = geoPoint
                    title = location.alias
                    icon = if (hasActiveReminderInRange) activeMarkerIcon else inactiveMarkerIcon
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
