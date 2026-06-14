package dev.arrase.geonotes.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private const val STEP_LOCATION = 0
private const val STEP_NOTIFICATION = 1
private const val STEP_BACKGROUND = 2
private const val STEP_DONE = 3

@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(STEP_LOCATION) }
    var showBackgroundDialog by remember { mutableStateOf(false) }

    // Check if location is already granted on first composition
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            val bgGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            step = when {
                bgGranted -> STEP_DONE
                else -> STEP_NOTIFICATION
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        step = if (fineGranted) STEP_NOTIFICATION else STEP_DONE
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Whether granted or not, move to background step
        val bgGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (bgGranted) {
            step = STEP_DONE
        } else {
            showBackgroundDialog = true
        }
    }

    when (step) {
        STEP_LOCATION -> {
            PermissionRequestScreen(message = "GeoNotes needs location access to create geofence reminders.")
            LaunchedEffect(Unit) {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        STEP_NOTIFICATION -> {
            PermissionRequestScreen(message = "GeoNotes needs notification permission to alert you when entering or leaving locations.")
            LaunchedEffect(Unit) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        STEP_BACKGROUND -> {
            // User was sent to settings; we show content and hope they granted it
            step = STEP_DONE
        }

        STEP_DONE -> content()
    }

    if (showBackgroundDialog) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundDialog = false
                step = STEP_DONE
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Background Location") },
            text = {
                Text(
                    "For geofence reminders to work reliably, GeoNotes needs " +
                            "\"Allow all the time\" location access. Please select it in the next screen."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundDialog = false
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                    step = STEP_DONE
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundDialog = false
                    step = STEP_DONE
                }) {
                    Text("Skip")
                }
            }
        )
    }
}

@Composable
private fun PermissionRequestScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
