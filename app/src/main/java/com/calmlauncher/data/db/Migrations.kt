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

/**
 * v9 makes reminder delivery exactly-once and puts the app-limit extension budget on the rule.
 *
 * Both columns exist to stop a bypass of a different kind:
 *
 *  - `reminders.lastFiredOccurrenceEpochMs` records which occurrence has already been shown, so
 *    a reboot, an app update, or a duplicate alarm delivery can no longer re-announce it. NULL
 *    for existing rows means "nothing announced yet", which is the safe default: the worst case
 *    is one notification for a reminder that is genuinely still outstanding.
 *
 *  - `app_limit_rules.override*` moves the extension ledger out of the event log and onto the
 *    rule, where it is read and written in the same transaction as the grant. Existing rows
 *    start at zero on a day-start of 0, which never matches today, so they simply reset.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `reminders` ADD COLUMN `lastFiredOccurrenceEpochMs` INTEGER")
        db.execSQL(
            "ALTER TABLE `app_limit_rules` ADD COLUMN `overrideDayStartEpochMs` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE `app_limit_rules` ADD COLUMN `overridesUsedToday` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE `app_limit_rules` ADD COLUMN `overrideMinutesUsedToday` INTEGER NOT NULL DEFAULT 0",
        )
    }
}

/** Every migration the database knows about, in order. */
val CALM_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
