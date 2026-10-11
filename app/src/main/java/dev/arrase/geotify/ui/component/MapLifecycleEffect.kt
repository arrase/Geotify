package dev.arrase.geotify.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.views.MapView

/**
 * Forwards lifecycle resume/pause to [map]. `onDetach()` shuts down the MapView's own tile-download
 * executor, so it is called whenever the view leaves composition (e.g. the user toggles back to the
 * list) — otherwise each toggle leaks a thread pool. It is safe to call repeatedly: osmdroid's tile
 * writer shares a static database, so detaching does not break later MapView instances.
 */
@Composable
fun MapLifecycleEffect(map: MapView?) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(map, lifecycle) {
        val mapView = map ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
}
