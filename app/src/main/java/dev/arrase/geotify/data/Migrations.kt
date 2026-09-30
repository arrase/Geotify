package dev.arrase.geotify.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema history for [GeotifyDatabase].
 *
 * Every step from the first released version is declared here so that upgrading never destroys
 * user data. Adding a version means bumping [GeotifyDatabase.version] and appending the matching
 * entry here.
 */
internal object Migrations {

    /** Ordered migration chain, from the first released schema to the current one. */
    fun all(): Array<Migration> = arrayOf(V1_TO_V2, V2_TO_V3, V3_TO_V4, V4_TO_V5)

    /** v2: composite index backing the spatial-search bounding-box query. */
    private val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_locations_latitude_longitude " +
                    "ON locations (latitude, longitude)"
            )
        }
    }

    /** v3: `is_in_range`, tracking whether a reminder's geofence is currently monitored. */
    private val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE reminders ADD COLUMN is_in_range INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /** v4: index covering the active-reminder filter and its `ORDER BY`. */
    private val V3_TO_V4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_reminders_is_active_created_at " +
                    "ON reminders (is_active, created_at)"
            )
        }
    }

    /**
     * v5: data cleanup only, no schema change.
     *
     * Aliases used to be persisted exactly as typed, so rows created before the repository
     * started trimming them can carry leading or trailing whitespace (e.g. `"Mercadona "`).
     * The uniqueness index is `COLLATE NOCASE`, which ignores case but not surrounding spaces, so
     * such a row could not be found by its own alias and blocked creating the properly-spaced one.
     *
     * A row is left untouched when trimming it would either create a collision with another row
     * or leave the alias empty. The former UI compared aliases without trimming, so `"Shop"` and
     * `"Shop "` could both exist, and trimming those would violate the unique index and abort the
     * whole statement, leaving the app unable to open its database. Such rows keep their original
     * value for the user to fix, rather than being merged or blanked.
     */
    private val V4_TO_V5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // SQLite's TRIM() drops spaces only, while the repository trims any whitespace via
            // Kotlin's String.trim(), so the same character list is given explicitly to keep
            // existing rows consistent with newly saved ones.
            //
            // `locations.alias` must stay qualified inside the subquery: an unqualified `alias`
            // would bind to `other.alias`, making the guard always false and silently trimming
            // nothing.
            val outerTrim = "TRIM(locations.alias, ' ' || char(9) || char(10) || char(13))"
            db.execSQL(
                """
                UPDATE locations
                SET alias = $outerTrim
                WHERE locations.alias <> $outerTrim
                  AND $outerTrim <> ''
                  AND NOT EXISTS (
                      SELECT 1 FROM locations other
                      WHERE other.id <> locations.id
                        AND other.alias = $outerTrim COLLATE NOCASE
                  )
                """.trimIndent()
            )
        }
    }
}
