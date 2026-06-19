package dev.arrase.geotify.permission

import android.Manifest
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import android.content.pm.PackageManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.arrase.geotify.R

private const val STEP_LOCATION = 0
private const val STEP_NOTIFICATION = 1
private const val STEP_ACTIVITY_RECOGNITION = 2
private const val STEP_DONE = 3

@Composable
fun rememberBackgroundLocationGranted(): Boolean {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return isGranted
}

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

            val activityRecognitionGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            step = when {
                bgGranted && activityRecognitionGranted -> STEP_DONE
                !activityRecognitionGranted -> STEP_ACTIVITY_RECOGNITION
                else -> STEP_NOTIFICATION
            }
        }
    }

    // Re-check background location permission when returning from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && step == STEP_DONE) {
                val bgGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                // Background location is optional — no action needed if still not granted.
                // This hook exists so future logic can react to the grant status change.
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        val bgGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (bgGranted) {
            step = STEP_ACTIVITY_RECOGNITION
        } else {
            showBackgroundDialog = true
        }
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        step = STEP_ACTIVITY_RECOGNITION
    }

    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        step = STEP_DONE
    }

    when (step) {
        STEP_LOCATION -> {
            PermissionRequestScreen(message = stringResource(R.string.perm_location_message))
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
            PermissionRequestScreen(message = stringResource(R.string.perm_notification_message))
            LaunchedEffect(Unit) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        STEP_ACTIVITY_RECOGNITION -> {
            PermissionRequestScreen(message = stringResource(R.string.perm_activity_recognition_message))
            LaunchedEffect(Unit) {
                activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        STEP_DONE -> content()
    }

    if (showBackgroundDialog) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundDialog = false
                step = STEP_ACTIVITY_RECOGNITION
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.perm_bg_location_title)) },
            text = {
                Text(stringResource(R.string.perm_bg_location_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundDialog = false
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) {
                    Text(stringResource(R.string.btn_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundDialog = false
                    step = STEP_ACTIVITY_RECOGNITION
                }) {
                    Text(stringResource(R.string.btn_skip))
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
