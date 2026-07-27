package com.calmlauncher.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written migrations. The database is configured with a destructive fallback, so a
 * missing migration silently drops every table — including the user's screen-time history
 * and app-limit rules. Any schema change that only *adds* something should be migrated
 * properly rather than left to that fallback.
 *
 * The SQL must match what Room generates for the entity exactly (column order, affinity,
 * nullability, primary key), or Room's startup validation fails.
 */

/** v7 introduces reminders/tasks. Nothing existing is touched. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `dueAtEpochMs` INTEGER,
                `repeatRule` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `completedAtEpochMs` INTEGER,
                `createdAtEpochMs` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

/** Every migration the database knows about, in order. */
val CALM_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_6_7)
