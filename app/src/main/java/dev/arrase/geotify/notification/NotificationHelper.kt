package dev.arrase.geotify.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.arrase.geotify.MainActivity
import dev.arrase.geotify.R
import dev.arrase.geotify.ui.navigation.GeotifyTab

object NotificationHelper {

    const val CHANNEL_GEOFENCE = "geofence_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val geofenceChannel = NotificationChannel(
                CHANNEL_GEOFENCE,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }

            manager.createNotificationChannels(listOf(geofenceChannel))
        }
    }

    /**
     * Shows a geofence notification for a specific reminder.
     *
     * @param notificationId Android notification id. Also used as the [PendingIntent] request code,
     * so it must be unique per notification — reusing a value across two notifications makes the
     * second one overwrite the first's `PendingIntent` extras.
     */
    fun showGeofenceNotification(
        context: Context,
        notificationId: Int,
        alias: String,
        message: String
    ) {
        // `POST_NOTIFICATIONS` is a runtime permission only from Android 13 (API 33). On older
        // versions the permission does not exist and checking it always reports denied, so the
        // check must be skipped entirely or notifications would never be posted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TAB, GeotifyTab.Reminders.name)
        }
        val openPending = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alias)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
