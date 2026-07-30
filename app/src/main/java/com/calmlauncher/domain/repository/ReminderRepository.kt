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
     * Atomically claim the right to announce [id]'s current occurrence, returning the reminder
     * when the claim succeeded and null when it did not.
     *
     * Every path that wants to *show* a reminder must go through here rather than reading the
     * reminder and deciding for itself. Null means someone already announced this occurrence —
     * a duplicate alarm delivery, a boot sweep racing the alarm itself, or the reminder having
     * been completed in the meantime — and the caller must stay silent.
     */
    suspend fun claimDueOccurrence(id: Long): Reminder?

    /**
     * Mark done. A repeating reminder isn't finished by completing one occurrence — it rolls
     * forward to its next due time and stays open instead.
     */
    suspend fun setCompleted(id: Long, completed: Boolean)

    /**
     * Roll a repeating reminder onto its next occurrence and re-arm it, without touching
     * anything currently on screen for it. Called the moment an occurrence is delivered, so the
     * series keeps running whether or not the user ever acts on this one. A no-op for reminders
     * that don't repeat.
     */
    suspend fun advanceRepeating(id: Long)

    /** Push a reminder's due time out by [minutes] from now. Clears the delivery claim. */
    suspend fun snooze(id: Long, minutes: Int)

    suspend fun delete(id: Long)

    suspend fun deleteCompleted()

    /** Re-arm every pending alarm. Called after boot, when the alarm store is wiped. */
    suspend fun rescheduleAll()
}
