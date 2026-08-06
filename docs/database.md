# Database Schema (`geotify.db`)

Geotify uses [Android Room](https://developer.android.com/training/data-storage/room) for local persistence. The database file is `geotify.db` (**Schema Version 3**).

---

## Entity Tables

### 1. `locations` (`LocationEntity`)

Stores points of interest (POIs) with alias names, GPS coordinates, and geofence parameters.

| Column | SQL Type | Constraints & Defaults | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | `PRIMARY KEY` | Unique UUID string. |
| `alias` | `TEXT` | `NOT NULL`, `COLLATE NOCASE`, `UNIQUE` | Unique location name (case-insensitive lookup). |
| `latitude` | `REAL` | `NOT NULL` | Latitude coordinate (`-90.0` to `90.0`). |
| `longitude` | `REAL` | `NOT NULL` | Longitude coordinate (`-180.0` to `180.0`). |
| `radius_meters` | `REAL` | `NOT NULL`, Default `150.0` | Geofence trigger radius in meters (min 50m). |
| `notification_responsiveness_ms` | `INTEGER` | `NOT NULL`, Default `0` | Geofence responsiveness delay hint in ms. |

**Indices**:
- `index_locations_alias`: Unique index on `alias` for fast case-insensitive lookups.
- `index_locations_latitude_longitude`: Composite index on `(latitude, longitude)` for spatial queries.

---

### 2. `reminders` (`ReminderEntity`)

Stores geofenced reminders linked to a location entity.

| Column | SQL Type | Constraints & Defaults | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | `PRIMARY KEY` | Unique UUID string. |
| `location_id` | `TEXT` | `NOT NULL`, `FK -> locations(id)` | Foreign key linking to the location. |
| `message` | `TEXT` | `NOT NULL` | Notification text displayed when triggered. |
| `transition_type` | `INTEGER` | `NOT NULL` | `1` = Enter (Arrival), `2` = Exit (Departure). |
| `is_active` | `INTEGER` | `NOT NULL`, Default `1` | Whether the reminder is active. |
| `created_at` | `INTEGER` | `NOT NULL` | Creation timestamp (epoch ms). |
| `is_in_range` | `INTEGER` | `NOT NULL`, Default `0` | Flag indicating whether device is inside fence. |

**Foreign Key & Cascading Deletion**:
- `foreignKeys = [ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["location_id"], onDelete = CASCADE)]`
- Deleting a location automatically deletes all associated reminders in SQLite.

---

## Key DAO Queries & Atomic Transactions

Definitions in [`LocationDao.kt`](file:///home/arrase/Develop/Geotify/app/src/main/java/dev/arrase/geotify/data/dao/LocationDao.kt) and [`ReminderDao.kt`](file:///home/arrase/Develop/Geotify/app/src/main/java/dev/arrase/geotify/data/dao/ReminderDao.kt).

### Bounding-Box Spatial Query
To pre-filter candidate locations for sliding window recalculations, Room executes a bounding-box query handling International Date Line (180° meridian) wrap-around:

```sql
SELECT * FROM locations 
WHERE latitude BETWEEN :minLat AND :maxLat 
  AND (
    (:minLon <= :maxLon AND longitude BETWEEN :minLon AND :maxLon)
    OR 
    (:minLon > :maxLon AND (longitude >= :minLon OR longitude <= :maxLon))
  )
```

### Atomic Range Status Update (`@Transaction`)
When spatial recalculations occur, `updateInRangeStatus` clears and updates `is_in_range` flags inside a single atomic SQLite transaction:

```kotlin
@Transaction
suspend fun updateInRangeStatus(locationIds: List<String>) {
    clearAllInRange()
    if (locationIds.isNotEmpty()) {
        setInRangeForLocations(locationIds)
    }
}
```

---

## Thread Safety & Non-Blocking I/O

Repository operations (`LocationRepository` and `ReminderRepository`) enforce thread safety by injecting `@IoDispatcher CoroutineDispatcher` and executing DAO operations inside `withContext(ioDispatcher)`. UI components observe changes asynchronously via Room `Flow` streams.
