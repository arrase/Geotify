package dev.arrase.geotify.ui.component

import android.graphics.DashPathEffect
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import dev.arrase.geotify.R
import dev.arrase.geotify.data.entity.LocationEntity
import dev.arrase.geotify.data.entity.ReminderEntity
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

data class SpatialRecalculationArea(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val innerRadiusMeters: Float,
    val outerRadiusMeters: Float
)

data class ReminderMapViewData(
    val reminders: List<ReminderEntity>,
    val locations: List<LocationEntity>,
    val selectedLocation: LocationEntity?,
    val spatialArea: SpatialRecalculationArea,
    val currentUserLocation: Location?
)

private data class ReminderMarkerStyle(
    val activeIcon: Drawable,
    val inactiveIcon: Drawable,
    val activeColor: Int,
    val inactiveColor: Int,
    val activeFillColor: Int,
    val inactiveFillColor: Int
)

@Composable
fun ReminderMapView(
    data: ReminderMapViewData,
    onLocationSelected: (LocationEntity?) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Configure osmdroid cache (side effect, runs once)
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid")
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hasCentered by remember { mutableStateOf(false) }

    // Map reminders to unique locations they target
    val activeLocations = remember(data.reminders, data.locations) {
        val locationIds = data.reminders.map { it.locationId }.toSet()
        data.locations.filter { locationIds.contains(it.id) }
    }

    // Initial centering and zooming
    LaunchedEffect(activeLocations, data.spatialArea.latitude, data.spatialArea.longitude, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (!hasCentered && centerMapToBounds(map, activeLocations, data.spatialArea, data.currentUserLocation)) {
            hasCentered = true
        }
    }

    // Center on selectedLocation changes
    LaunchedEffect(data.selectedLocation, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        data.selectedLocation?.let {
            map.controller.animateTo(GeoPoint(it.latitude, it.longitude))
        }
    }

    MapLifecycleEffect(mapViewRef)

    // Custom pins and geofence colors. The active/inactive hues are semantic (they encode reminder
    // state, not the app theme), while map chrome follows the Material theme so it stays legible on
    // the darkened tiles used in dark mode.
    val activeColor = MaterialTheme.colorScheme.error.toArgb()
    val inactiveColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val slidingWindowColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val userLocationColor = MaterialTheme.colorScheme.primary.toArgb()

    val activeMarkerIcon = remember(context, activeColor) {
        getTintedMarkerIcon(context, activeColor, sizeDp = 38)
    }
    val inactiveMarkerIcon = remember(context, inactiveColor) {
        getTintedMarkerIcon(context, inactiveColor, sizeDp = 38)
    }
    val userMarkerIcon = remember(context, userLocationColor) {
        getTintedMarkerIcon(context, userLocationColor, sizeDp = 24)
    }
    val centerMarkerIcon = remember(context, slidingWindowColor) {
        getTintedMarkerIcon(context, slidingWindowColor, sizeDp = 20)
    }

    val activeFillColor = activeColor.withAlpha(FILL_ALPHA)
    val inactiveFillColor = inactiveColor.withAlpha(FILL_ALPHA)

    val labelSlidingWindowCenter = stringResource(R.string.label_sliding_window_center)
    val labelMyLocation = stringResource(R.string.label_my_location)

    val markerStyle = remember(
        activeMarkerIcon, inactiveMarkerIcon, activeColor, inactiveColor,
        activeFillColor, inactiveFillColor
    ) {
        ReminderMarkerStyle(
            activeIcon = activeMarkerIcon,
            inactiveIcon = inactiveMarkerIcon,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            activeFillColor = activeFillColor,
            inactiveFillColor = inactiveFillColor
        )
    }

    val tileFilter = remember { darkTileFilter() }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(15.0)
                onResume()
                mapViewRef = this
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { map ->
            map.applyTileThemeFilter(isDarkTheme, tileFilter)
            map.overlays.clear()

            // Deselect single tap listener
            val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    onLocationSelected(null)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            })
            map.overlays.add(mapEventsOverlay)

            drawSpatialCircles(
                map = map,
                spatialArea = data.spatialArea,
                centerMarkerIcon = centerMarkerIcon,
                centerLabel = labelSlidingWindowCenter,
                slidingWindowColor = slidingWindowColor
            )
            drawUserLocationMarker(map, data.currentUserLocation, userMarkerIcon, labelMyLocation)
            drawReminderMarkers(
                map = map,
                activeLocations = activeLocations,
                reminders = data.reminders,
                selectedLocation = data.selectedLocation,
                style = markerStyle,
                onLocationSelected = onLocationSelected
            )

            map.invalidate()
        }
    )
}

private fun centerMapToBounds(
    map: MapView,
    activeLocations: List<LocationEntity>,
    spatialArea: SpatialRecalculationArea,
    currentUserLocation: Location?
): Boolean {
    val points = mutableListOf<GeoPoint>()
    for (location in activeLocations) {
        points.add(GeoPoint(location.latitude, location.longitude))
    }
    val lat = spatialArea.latitude
    val lng = spatialArea.longitude
    if (lat != null && lng != null) {
        points.add(GeoPoint(lat, lng))
    }
    if (currentUserLocation != null) {
        points.add(GeoPoint(currentUserLocation.latitude, currentUserLocation.longitude))
    }

    if (points.isEmpty()) {
        return false
    }

    map.post {
        try {
            if (points.size == 1) {
                map.controller.setCenter(points.first())
                map.controller.setZoom(15.0)
            } else {
                val box = BoundingBox.fromGeoPoints(points)
                map.zoomToBoundingBox(box, true, 120)
            }
        } catch (e: Exception) {
            if (lat != null && lng != null) {
                map.controller.setCenter(GeoPoint(lat, lng))
                map.controller.setZoom(14.0)
            }
        }
    }
    return true
}


private fun drawSpatialCircles(
    map: MapView,
    spatialArea: SpatialRecalculationArea,
    centerMarkerIcon: Drawable,
    centerLabel: String,
    slidingWindowColor: Int
) {
    val lat = spatialArea.latitude
    val lng = spatialArea.longitude
    if (lat != null && lng != null) {
        val centerPoint = GeoPoint(lat, lng)

        // 1a. Outer radius — spatial search area (dashed, subtle)
        val outerCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(centerPoint, spatialArea.outerRadiusMeters.toDouble())
            fillPaint.color = slidingWindowColor.withAlpha(25)
            outlinePaint.color = slidingWindowColor.withAlpha(180)
            outlinePaint.strokeWidth = 3f
            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 15f), 0f)
        }
        map.overlays.add(outerCircle)

        // 1b. Inner radius — master geofence boundary (solid, prominent)
        val innerCircle = Polygon().apply {
            points = Polygon.pointsAsCircle(centerPoint, spatialArea.innerRadiusMeters.toDouble())
            fillPaint.color = slidingWindowColor.withAlpha(18)
            outlinePaint.color = slidingWindowColor
            outlinePaint.strokeWidth = 5f
        }
        map.overlays.add(innerCircle)

        // Master geofence center marker
        val centerMarker = Marker(map).apply {
            position = centerPoint
            title = centerLabel
            icon = centerMarkerIcon
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
        }
        map.overlays.add(centerMarker)
    }
}

private fun drawUserLocationMarker(
    map: MapView,
    currentUserLocation: Location?,
    userMarkerIcon: Drawable,
    label: String
) {
    if (currentUserLocation != null) {
        val userPoint = GeoPoint(currentUserLocation.latitude, currentUserLocation.longitude)
        val userMarker = Marker(map).apply {
            position = userPoint
            title = label
            icon = userMarkerIcon
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
        }
        map.overlays.add(userMarker)
    }
}

private fun drawLocationCircle(
    map: MapView,
    geoPoint: GeoPoint,
    radiusMeters: Float,
    isSelected: Boolean,
    hasActiveReminderInRange: Boolean,
    style: ReminderMarkerStyle
) {
    val circle = Polygon().apply {
        points = Polygon.pointsAsCircle(geoPoint, radiusMeters.toDouble())
        fillPaint.color = if (hasActiveReminderInRange) style.activeFillColor else style.inactiveFillColor
        outlinePaint.color = if (hasActiveReminderInRange) style.activeColor else style.inactiveColor
        outlinePaint.strokeWidth = if (isSelected) 8f else (if (hasActiveReminderInRange) 5f else 3f)
    }
    map.overlays.add(circle)
}

private fun drawLocationMarker(
    map: MapView,
    location: LocationEntity,
    geoPoint: GeoPoint,
    hasActiveReminderInRange: Boolean,
    style: ReminderMarkerStyle,
    onLocationSelected: (LocationEntity) -> Unit
) {
    val marker = Marker(map).apply {
        position = geoPoint
        title = location.alias
        icon = if (hasActiveReminderInRange) style.activeIcon else style.inactiveIcon
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        setInfoWindow(null)

        setOnMarkerClickListener { _, _ ->
            onLocationSelected(location)
            true
        }
    }
    map.overlays.add(marker)
}

private fun drawReminderMarkers(
    map: MapView,
    activeLocations: List<LocationEntity>,
    reminders: List<ReminderEntity>,
    selectedLocation: LocationEntity?,
    style: ReminderMarkerStyle,
    onLocationSelected: (LocationEntity) -> Unit
) {
    for (location in activeLocations) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val isSelected = selectedLocation?.id == location.id
        val hasActiveReminderInRange = reminders.any { it.locationId == location.id && it.isInRange }

        drawLocationCircle(
            map = map,
            geoPoint = geoPoint,
            radiusMeters = location.radiusMeters,
            isSelected = isSelected,
            hasActiveReminderInRange = hasActiveReminderInRange,
            style = style
        )
        drawLocationMarker(
            map = map,
            location = location,
            geoPoint = geoPoint,
            hasActiveReminderInRange = hasActiveReminderInRange,
            style = style,
            onLocationSelected = onLocationSelected
        )
    }
}

/** Opacity applied to the semi-transparent fill inside a geofence circle. */
private const val FILL_ALPHA = 55

/** Returns this opaque ARGB color with only its alpha replaced. */
private fun Int.withAlpha(alpha: Int): Int =
    (this and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
