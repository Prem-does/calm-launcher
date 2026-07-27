package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Reminders/tasks. Every mutation here also keeps the alarm schedule in sync, so callers
 * never have to remember to (re)schedule — saving a reminder is enough for it to fire.
 */
interface ReminderRepository {
    fun observeAll(): Flow<List<Reminder>>

    suspend fun get(id: Long): Reminder?

    /** Insert or update, returning the reminder's id. Reschedules its alarm. */
    suspend fun save(reminder: Reminder): Long

    /**
     * Mark done. A repeating reminder isn't finished by completing one occurrence — it rolls
     * forward to its next due time and stays open instead.
     */
    suspend fun setCompleted(id: Long, completed: Boolean)

    /** Push a reminder's due time out by [minutes] from now. */
    suspend fun snooze(id: Long, minutes: Int)

    suspend fun delete(id: Long)

    suspend fun deleteCompleted()

    /** Re-arm every pending alarm. Called after boot, when the alarm store is wiped. */
    suspend fun rescheduleAll()
}
