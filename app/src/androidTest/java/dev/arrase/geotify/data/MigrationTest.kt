package dev.arrase.geotify.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real SQL of every migration step against a database built from the exported
 * schema, on a device.
 *
 * The alias-trimming step is covered exhaustively because it is easy to write a correlated
 * subquery that silently matches nothing: the migration then completes, the app opens fine, and
 * nothing is cleaned up at all. Only real SQLite can catch that.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        // Only used to locate the exported schemas under the test APK's assets, which Room
        // writes under the database class name. The database file itself is named per test.
        GeotifyDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migratesFromV1ToCurrent() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO locations
                    (id, alias, latitude, longitude, radius_meters, notification_responsiveness_ms)
                VALUES ('l1', 'Casa', 39.95, -0.06, 100.0, 0)
                """.trimIndent()
            )
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB, CURRENT_VERSION, validateDroppedTables = true, *Migrations.all()
        )

        // runMigrationsAndValidate checks the schema; the row must also have survived the chain.
        assertEquals(listOf("Casa"), migrated.readAliases())
        migrated.close()
    }

    @Test
    fun v4ToV5_trimsAliasesThatGainedWhitespace() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            listOf(
                "l1" to "Casa",
                "l2" to "Consum ",
                "l3" to "Mercadona ",
                "l4" to "\tPipican\n",
                "l5" to "  Plaza  "
            ).forEach { (id, alias) -> db.insertLocation(id, alias) }
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB, CURRENT_VERSION, validateDroppedTables = true, *Migrations.all()
        )

        assertEquals(
            listOf("Casa", "Consum", "Mercadona", "Pipican", "Plaza"),
            migrated.readAliases()
        )
        migrated.close()
    }

    @Test
    fun v4ToV5_keepsCollidingAliasesSoTheUniqueIndexIsNotViolated() {
        // The old duplicate check compared aliases without trimming, so both could exist.
        helper.createDatabase(TEST_DB, 4).use { db ->
            listOf("l1" to "Shop", "l2" to "Shop ", "l3" to "Cafe ")
                .forEach { (id, alias) -> db.insertLocation(id, alias) }
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB, CURRENT_VERSION, validateDroppedTables = true, *Migrations.all()
        )

        // 'Shop' vs 'Shop ' collide and are both left alone; the unrelated row is still fixed.
        assertEquals(listOf("Shop", "Shop ", "Cafe"), migrated.readAliases())
        migrated.close()
    }

    @Test
    fun v4ToV5_doesNotBlankAnAllWhitespaceAlias() {
        helper.createDatabase(TEST_DB, 4).use { db -> db.insertLocation("l1", "   ") }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB, CURRENT_VERSION, validateDroppedTables = true, *Migrations.all()
        )

        assertEquals(listOf("   "), migrated.readAliases())
        migrated.close()
    }

    @Test
    fun v4ToV5_leavesCleanAliasesUntouched() {
        helper.createDatabase(TEST_DB, 4).use { db -> db.insertLocation("l1", "Mi Casa Bonita") }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB, CURRENT_VERSION, validateDroppedTables = true, *Migrations.all()
        )

        assertEquals(listOf("Mi Casa Bonita"), migrated.readAliases())
        migrated.close()
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertLocation(id: String, alias: String) {
        execSQL(
            "INSERT INTO locations " +
                "(id, alias, latitude, longitude, radius_meters, notification_responsiveness_ms) " +
                "VALUES ('$id', '${alias.replace("'", "''")}', 39.95, -0.06, 100.0, 0)"
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.readAliases(): List<String> =
        query("SELECT alias FROM locations ORDER BY rowid").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    private companion object {
        const val TEST_DB = "migration-test"
        const val CURRENT_VERSION = 5
    }
}
