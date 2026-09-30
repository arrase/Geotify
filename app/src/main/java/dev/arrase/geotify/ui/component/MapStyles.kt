package dev.arrase.geotify.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import dev.arrase.geotify.R
import org.osmdroid.views.MapView

/**
 * Colour filter that inverts and darkens the map tiles so they blend with the dark UI theme.
 * Build it once inside a `remember`; [MapView.applyTileThemeFilter] only applies it for the dark
 * theme and clears it otherwise.
 */
fun darkTileFilter(): ColorMatrixColorFilter = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            -0.1491f, -0.5005f, -0.0504f, 0f, 215f,
            -0.1491f, -0.5005f, -0.0504f, 0f, 215f,
            -0.1491f, -0.5005f, -0.0504f, 0f, 230f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/**
 * Applies the dark-tile filter, or clears it for the light theme. Build the filter once with
 * [darkTileFilter] inside a `remember` and pass it in, so it is not reallocated on every
 * recomposition.
 */
fun MapView.applyTileThemeFilter(isDarkTheme: Boolean, filter: ColorMatrixColorFilter?) {
    overlayManager.tilesOverlay.setColorFilter(if (isDarkTheme) filter else null)
}

/**
 * Marker icon tinted with [color]. Building one allocates a bitmap, so results are cached by
 * colour, size and screen density.
 */
private val markerIconCache = LruCache<String, Drawable>(32)

fun getTintedMarkerIcon(context: Context, color: Int, sizeDp: Int = 38): Drawable {
    val density = context.resources.displayMetrics.density
    val cacheKey = "$color@${sizeDp}dp@${density}x"
    markerIconCache.get(cacheKey)?.let { return it }

    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location)
        ?: return color.toDrawable()

    val size = (sizeDp * density).toInt()
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val mutated = drawable.mutate()
    DrawableCompat.setTint(mutated, color)
    mutated.setBounds(0, 0, size, size)
    mutated.draw(Canvas(bitmap))

    return bitmap.toDrawable(context.resources).also { markerIconCache.put(cacheKey, it) }
}
