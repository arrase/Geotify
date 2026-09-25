package dev.arrase.geotify.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import dev.arrase.geotify.R
import org.osmdroid.views.MapView

fun applyTileThemeFilter(map: MapView, isDarkTheme: Boolean) {
    if (isDarkTheme) {
        val filter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    -0.1491f, -0.5005f, -0.0504f, 0f, 215f,
                    -0.1491f, -0.5005f, -0.0504f, 0f, 215f,
                    -0.1491f, -0.5005f, -0.0504f, 0f, 230f,
                    0f,        0f,        0f,        1f, 0f
                )
            )
        )
        map.overlayManager.tilesOverlay.setColorFilter(filter)
    } else {
        map.overlayManager.tilesOverlay.setColorFilter(null)
    }
}

fun getTintedMarkerIcon(context: Context, color: Int, sizeDp: Int = 38): Drawable {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location)
        ?: return color.toDrawable()
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt()
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    val mutated = drawable.mutate()
    DrawableCompat.setTint(mutated, color)
    mutated.draw(canvas)
    return bitmap.toDrawable(context.resources)
}
