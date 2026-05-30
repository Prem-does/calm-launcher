package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Append-only record of an app open. Mirrors [com.calmlauncher.domain.model.LaunchEvent]. */
@Entity(tableName = "launch_event")
data class LaunchEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val category: String,
    val timestampEpochMs: Long,
    val reason: String? = null,
    val source: String,
)
