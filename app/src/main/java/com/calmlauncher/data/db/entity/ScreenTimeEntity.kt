package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Foreground usage for a single day, keyed by the day's start (local midnight) in epoch
 * millis. Per-app breakdown is persisted as a JSON object string (package -> millis) to
 * avoid pulling in a serialization dependency.
 */
@Entity(tableName = "screen_time")
data class ScreenTimeEntity(
    @PrimaryKey val dayStartEpochMs: Long,
    val totalForegroundMs: Long,
    val perAppJson: String,
)
