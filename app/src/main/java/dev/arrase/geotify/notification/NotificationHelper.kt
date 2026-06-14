package dev.arrase.geotify.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.arrase.geotify.MainActivity
import dev.arrase.geotify.R

object NotificationHelper {

    const val CHANNEL_GEOFENCE = "geofence_reminders"
    const val CHANNEL_LOCATION_SERVICE = "location_service"
    const val ACTION_DISMISS_REMINDER = "dev.arrase.geotify.ACTION_DISMISS_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val LOCATION_SERVICE_NOTIFICATION_ID = 1

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val geofenceChannel = NotificationChannel(
            CHANNEL_GEOFENCE,
            "Geofence Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications triggered when you enter or leave a saved location"
        }

        val locationServiceChannel = NotificationChannel(
            CHANNEL_LOCATION_SERVICE,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while obtaining your current GPS location"
        }

        manager.createNotificationChannels(listOf(geofenceChannel, locationServiceChannel))
    }

    fun buildLocationServiceNotification(context: Context): android.app.Notification =
        NotificationCompat.Builder(context, CHANNEL_LOCATION_SERVICE)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle("Obtaining location")
            .setContentText("Getting your current GPS coordinates…")
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun showGeofenceNotification(
        context: Context,
        notificationId: Int,
        reminderId: String,
        alias: String,
        message: String
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("tab", "reminders")
        }
        val openPending = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(ACTION_DISMISS_REMINDER).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, reminderId.hashCode(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GEOFENCE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(alias)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPending)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
