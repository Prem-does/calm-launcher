package com.calmlauncher.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persisted reminder/task. Mirrors [com.calmlauncher.domain.model.Reminder]. */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String,
    val dueAtEpochMs: Long?,
    /** Stored as the [com.calmlauncher.domain.model.RepeatRule] name. */
    val repeatRule: String,
    val completed: Boolean,
    val completedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
)
