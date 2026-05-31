package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_session")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayStartEpochMs: Long,
    val packageName: String,
    val appName: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val durationMinutes: Int,
)
