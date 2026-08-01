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
    /**
     * The `dueAtEpochMs` of the occurrence that has already been announced to the user.
     *
     * This is the duplicate-notification guard, and it is a *value* rather than a flag on
     * purpose: an occurrence is identified by the instant it was due, so claiming one is a
     * single atomic UPDATE ([com.calmlauncher.data.db.ReminderDao.claimOccurrence]) that can
     * only succeed once no matter how many independent callers race for it — the exact alarm,
     * a duplicate alarm delivery, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, or a reschedule
     * sweep after the launcher is reopened.
     *
     * A repeating reminder rolls its due time forward when it fires, so the next occurrence
     * carries a different value and is free to announce itself. Snoozing likewise moves the
     * due time, which is what makes a snoozed reminder eligible to speak again.
     */
    val lastFiredOccurrenceEpochMs: Long? = null,
)
