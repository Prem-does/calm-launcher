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

/**
 * v8 records *which* limit notification an app has already been shown today, so the exact
 * alarm and the usage rollup stop posting duplicates of the same message. Purely additive:
 * existing rules default to "nothing said yet", which simply lets the next sync speak once.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `app_limit_rules` ADD COLUMN `lastNotifiedStage` TEXT NOT NULL DEFAULT 'NONE'",
        )
        db.execSQL(
            "ALTER TABLE `app_limit_rules` ADD COLUMN `lastNotifiedDayStartEpochMs` INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/** Every migration the database knows about, in order. */
val CALM_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_6_7, MIGRATION_7_8)
