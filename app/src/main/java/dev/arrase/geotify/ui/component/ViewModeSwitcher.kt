package dev.arrase.geotify.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.R

@Composable
fun ViewModeSwitcher(
    isMapView: Boolean,
    onListSelected: () -> Unit,
    onMapSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        val containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewModeTabItem(
                    title = stringResource(R.string.tab_list),
                    isSelected = !isMapView,
                    onClick = onListSelected
                )
                ViewModeTabItem(
                    title = stringResource(R.string.tab_map),
                    isSelected = isMapView,
                    onClick = onMapSelected
                )
            }
        }
    }
}

@Composable
private fun ViewModeTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val onSelectedColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedColor else Color.Transparent,
        modifier = Modifier
            .width(100.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) onSelectedColor else contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
