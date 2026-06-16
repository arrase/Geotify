package dev.arrase.geotify.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.arrase.geotify.R
import dev.arrase.geotify.data.ThemeSetting

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val appTheme by viewModel.appTheme.collectAsState()
    val mapTheme by viewModel.mapTheme.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsSectionHeader(
            title = stringResource(R.string.label_settings),
            subtitle = stringResource(R.string.settings_subtitle)
        )

        // App Theme Settings Card
        SettingsCard(
            icon = Icons.Filled.DarkMode,
            title = stringResource(R.string.settings_app_theme_title),
            description = stringResource(R.string.settings_app_theme_desc)
        ) {
            ThemeSelector(
                selectedTheme = appTheme,
                onThemeSelected = { viewModel.setAppTheme(it) }
            )
        }

        // Map Theme Settings Card
        SettingsCard(
            icon = Icons.Filled.Map,
            title = stringResource(R.string.settings_map_theme_title),
            description = stringResource(R.string.settings_map_theme_desc)
        ) {
            ThemeSelector(
                selectedTheme = mapTheme,
                onThemeSelected = { viewModel.setMapTheme(it) }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: ThemeSetting,
    onThemeSelected: (ThemeSetting) -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    val selectedColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val onSelectedColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeSetting.entries.forEach { theme ->
                val isSelected = selectedTheme == theme
                val label = when (theme) {
                    ThemeSetting.SYSTEM -> stringResource(R.string.settings_theme_system)
                    ThemeSetting.LIGHT -> stringResource(R.string.settings_theme_light)
                    ThemeSetting.DARK -> stringResource(R.string.settings_theme_dark)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) selectedColor else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onThemeSelected(theme) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) onSelectedColor else contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
