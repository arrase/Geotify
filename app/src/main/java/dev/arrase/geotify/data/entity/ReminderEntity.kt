package dev.arrase.geotify.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.gms.location.Geofence

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["location_id"]), Index(value = ["is_active", "created_at"])]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "location_id")
    val locationId: String,
    val message: String,
    @ColumnInfo(name = "transition_type")
    val transitionType: Int,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "is_in_range", defaultValue = "0")
    val isInRange: Boolean = false
)

val ReminderEntity.isArrival: Boolean
    get() = transitionType == Geofence.GEOFENCE_TRANSITION_ENTER

val ReminderEntity.isDeparture: Boolean
    get() = transitionType == Geofence.GEOFENCE_TRANSITION_EXIT

/** Stable, human-readable label for [transitionType], used by App Functions responses. */
val ReminderEntity.triggerTypeString: String
    get() = when (transitionType) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> "arrival"
        Geofence.GEOFENCE_TRANSITION_EXIT -> "departure"
        else -> "unknown"
    }

/**
 * Stable Android notification id for this reminder.
 *
 * Notification ids double as `PendingIntent` request codes, so two reminders sharing an id would
 * make the second notification overwrite the first one's intent extras. Reminders are created one
 * at a time by the user, so a millisecond timestamp is unique in practice and cheap to derive.
 */
fun ReminderEntity.notificationId(): Int = createdAt.toInt() xor id.hashCode()

data class LocationReminderCount(
    @ColumnInfo(name = "location_id") val locationId: String,
    val count: Int
)
