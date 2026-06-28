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
    const val EXTRA_TAB = "tab"
    const val TAB_REMINDERS = "reminders"

    fun createNotificationChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
     * @param notificationId An integer ID for this notification. Note: If using `reminderId.hashCode()`,
     *                       there is a small theoretical risk of hash collision where another active reminder
     *                       shares the same hashCode and overwrites its notification. For this app, this is
     *                       a reasonable trade-off to map string UUIDs to 32-bit Android notification IDs,
     *                       as the number of simultaneously active notifications is small.
     */
    fun showGeofenceNotification(
        context: Context,
        notificationId: Int,
        alias: String,
        message: String
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TAB, TAB_REMINDERS)
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
