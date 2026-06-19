package dev.arrase.geotify.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index(value = ["location_id"])]
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
    get() = transitionType == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER

val ReminderEntity.transitionLabel: String
    get() = if (isArrival) "↓ Arrival" else "↑ Departure"

val ReminderEntity.triggerTypeString: String
    get() = if (isArrival) "arrival" else "departure"

data class LocationReminderCount(
    @ColumnInfo(name = "location_id") val locationId: String,
    val count: Int
)

