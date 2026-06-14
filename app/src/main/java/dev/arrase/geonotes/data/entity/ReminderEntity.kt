package dev.arrase.geonotes.data.entity

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
    val createdAt: Long
)
