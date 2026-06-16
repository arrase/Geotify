package dev.arrase.geotify.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [Index(value = ["alias"], unique = true)]
)
data class LocationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val alias: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Float = 150f,
    @ColumnInfo(name = "notification_responsiveness_ms")
    val notificationResponsivenessMs: Int = 0
)
