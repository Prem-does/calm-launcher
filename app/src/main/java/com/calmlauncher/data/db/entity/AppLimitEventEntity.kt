package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limit_events")
data class AppLimitEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val label: String,
    val eventType: String,
    val timestampEpochMs: Long,
    val dayStartEpochMs: Long,
    val limitMinutes: Int,
    val usedMinutes: Int,
    val overrideMinutes: Int,
)
