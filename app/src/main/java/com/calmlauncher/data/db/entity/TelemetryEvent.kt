package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telemetry_events")
data class TelemetryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val details: String,
    val timestampMillis: Long
)
