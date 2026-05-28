package com.calmlauncher.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object CalmDatabaseProvider {
    @Volatile
    private var instance: CalmDatabase? = null

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `launch_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `packageName` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `reason` TEXT NOT NULL,
                    `timestampMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `telemetry_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL,
                    `details` TEXT NOT NULL,
                    `timestampMillis` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    fun get(context: Context): CalmDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CalmDatabase::class.java,
                "calm-launcher.db"
            ).addMigrations(migration2To3, migration3To4).build().also { instance = it }
        }
    }
}
