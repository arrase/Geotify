package dev.arrase.geotify.ui.component

import android.graphics.drawable.Drawable
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

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Centering state
    var hasCentered by remember { mutableStateOf(false) }

    // Initial centering and zooming: fits bounds if multiple locations, centers on first if single
    LaunchedEffect(locations, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (!hasCentered && locations.isNotEmpty()) {
            if (locations.size == 1) {
                val loc = locations.first()
                map.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                map.controller.setZoom(15.0)
            } else {
                val points = locations.map { GeoPoint(it.latitude, it.longitude) }
                map.post {
                    try {
                        val box = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
                        map.zoomToBoundingBox(box, true, 120)
                    } catch (e: Exception) {
                        val loc = locations.first()
                        map.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
                        map.controller.setZoom(15.0)
                    }
                }
            }
            hasCentered = true
        }
    }

    // Smoothly animate centering when selectedLocation changes
    LaunchedEffect(selectedLocation, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        selectedLocation?.let {
            map.controller.animateTo(GeoPoint(it.latitude, it.longitude))
        }
    }

    // Lifecycle management: onDetach() must only be called on real Activity destruction,
    // NOT when AnimatedVisibility hides this composable, because onDetach() permanently
    // destroys the osmdroid tile cache writer making future MapView instances unable to load tiles.
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
            // Only pause when leaving composition (e.g. AnimatedVisibility toggling).
            // Do NOT call onDetach() here — it destroys the shared tile cache writer.
            map.onPause()
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

    val overlayStyle = remember(selectedMarkerIcon, defaultMarkerIcon, selectedFillColor, selectedStrokeColor, defaultFillColor, defaultStrokeColor) {
        LocationOverlayStyle(
            selectedMarkerIcon = selectedMarkerIcon,
            defaultMarkerIcon = defaultMarkerIcon,
            selectedFillColor = selectedFillColor,
            selectedStrokeColor = selectedStrokeColor,
            defaultFillColor = defaultFillColor,
            defaultStrokeColor = defaultStrokeColor
        )
    }

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
            applyTileThemeFilter(map, isDarkTheme)
            updateLocationOverlays(
                map = map,
                locations = locations,
                selectedLocation = selectedLocation,
                style = overlayStyle,
                onLocationSelected = onLocationSelected
            )
        }
    )
}

private data class LocationOverlayStyle(
    val selectedMarkerIcon: Drawable,
    val defaultMarkerIcon: Drawable,
    val selectedFillColor: Int,
    val selectedStrokeColor: Int,
    val defaultFillColor: Int,
    val defaultStrokeColor: Int
)


private fun updateLocationOverlays(
    map: MapView,
    locations: List<LocationEntity>,
    selectedLocation: LocationEntity?,
    style: LocationOverlayStyle,
    onLocationSelected: (LocationEntity?) -> Unit
) {
    map.overlays.clear()

    val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
            onLocationSelected(null)
            return true
        }

        override fun longPressHelper(p: GeoPoint): Boolean = false
    })
    map.overlays.add(mapEventsOverlay)

    locations.forEach { location ->
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val isSelected = selectedLocation?.id == location.id

        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(geoPoint, location.radiusMeters.toDouble())
            fillPaint.color = if (isSelected) style.selectedFillColor else style.defaultFillColor
            outlinePaint.color = if (isSelected) style.selectedStrokeColor else style.defaultStrokeColor
            outlinePaint.strokeWidth = if (isSelected) 5f else 3f
        }
        map.overlays.add(circle)

        val marker = Marker(map).apply {
            position = geoPoint
            title = location.alias
            icon = if (isSelected) style.selectedMarkerIcon else style.defaultMarkerIcon
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
