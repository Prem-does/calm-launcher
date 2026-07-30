package com.calmlauncher.domain.model

import java.util.Calendar

/** How often a reminder repeats after it fires. */
enum class RepeatRule { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

/**
 * A single reminder/task, modelled on Samsung Reminder: a title, optional note, an optional
 * due time, an optional repeat rule, and a completed flag.
 *
 * A reminder with no [dueAtEpochMs] is a plain to-do — it sits in the list and never alarms.
 * One with a due time schedules an exact alarm; when it repeats, firing advances the due
 * time to the next occurrence rather than completing the reminder.
 */
data class Reminder(
    val id: Long = 0L,
    val title: String,
    val note: String = "",
    val dueAtEpochMs: Long? = null,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val completed: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = 0L,
    /**
     * Due time of the occurrence the user has already been shown. See
     * [com.calmlauncher.data.db.entity.ReminderEntity.lastFiredOccurrenceEpochMs] — this is the
     * single piece of state that makes firing idempotent.
     */
    val lastFiredOccurrenceEpochMs: Long? = null,
) {
    val hasDueTime: Boolean get() = dueAtEpochMs != null

    /** True when this reminder's *current* occurrence has already been announced. */
    val currentOccurrenceAlreadyFired: Boolean
        get() = dueAtEpochMs != null && dueAtEpochMs == lastFiredOccurrenceEpochMs

    /** True when the due time has passed and the reminder is still open. */
    fun isOverdue(nowEpochMs: Long = System.currentTimeMillis()): Boolean =
        !completed && dueAtEpochMs != null && dueAtEpochMs <= nowEpochMs

    /**
     * The next due time after [fromEpochMs] for a repeating reminder, or null when it doesn't
     * repeat. Uses [Calendar] arithmetic so month/year steps land on the same calendar day
     * (and clamp sensibly for the 31st of a short month) rather than drifting by fixed millis.
     */
    fun nextOccurrence(fromEpochMs: Long = System.currentTimeMillis()): Long? {
        val due = dueAtEpochMs ?: return null
        if (repeatRule == RepeatRule.NONE) return null

        val calendar = Calendar.getInstance().apply { timeInMillis = due }
        // Step forward until we're past `from`, so a reminder missed for days still lands
        // on its next real occurrence instead of firing repeatedly for every one it missed.
        do {
            when (repeatRule) {
                RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RepeatRule.YEARLY -> calendar.add(Calendar.YEAR, 1)
                RepeatRule.NONE -> return null
            }
        } while (calendar.timeInMillis <= fromEpochMs)
        return calendar.timeInMillis
    }
}
