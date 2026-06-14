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
    const val ACTION_DISMISS_REMINDER = "dev.arrase.geotify.ACTION_DISMISS_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TAB = "tab"
    const val TAB_REMINDERS = "reminders"

    fun createNotificationChannels(context: Context) {
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
            putExtra(EXTRA_TAB, TAB_REMINDERS)
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
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_dismiss, context.getString(R.string.notif_action_dismiss), dismissPending)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
