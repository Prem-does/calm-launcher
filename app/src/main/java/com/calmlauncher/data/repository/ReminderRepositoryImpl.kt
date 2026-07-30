package com.calmlauncher.data.repository

import com.calmlauncher.data.db.ReminderDao
import com.calmlauncher.data.db.entity.ReminderEntity
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.domain.model.RepeatRule
import com.calmlauncher.domain.repository.ReminderRepository
import com.calmlauncher.notification.ReminderNotificationManager
import com.calmlauncher.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Room-backed reminders. Persisting and scheduling are deliberately kept together: every
 * write re-arms (or cancels) the reminder's alarm in the same operation, so the database and
 * the alarm queue can't drift apart.
 */
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler,
    private val notifications: ReminderNotificationManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ReminderRepository {

    override fun observeAll(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(dispatcher)

    override suspend fun get(id: Long): Reminder? = withContext(dispatcher) {
        reminderDao.get(id)?.toDomain()
    }

    override suspend fun save(reminder: Reminder): Long = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        if (reminder.id == 0L) {
            val id = reminderDao.insert(
                reminder.copy(
                    createdAtEpochMs = now,
                    lastFiredOccurrenceEpochMs = null,
                ).toEntity(),
            )
            scheduler.schedule(reminder.copy(id = id, lastFiredOccurrenceEpochMs = null))
            return@withContext id
        }

        // Preserve the delivery claim across an edit *unless* the due time actually moved.
        // Carrying it means re-saving a reminder (renaming it, adding a note) can't resurrect a
        // notification the user has already dealt with; dropping it when the time changes means
        // a genuinely rescheduled reminder is free to fire again.
        val existing = reminderDao.get(reminder.id)?.toDomain()
        val dueChanged = existing?.dueAtEpochMs != reminder.dueAtEpochMs
        val carriedClaim = if (dueChanged) null else existing?.lastFiredOccurrenceEpochMs
        val updated = reminder.copy(lastFiredOccurrenceEpochMs = carriedClaim)

        reminderDao.update(updated.toEntity())
        // An edit supersedes whatever is in the shade for this reminder.
        notifications.cancel(reminder.id)
        scheduler.schedule(updated)
        reminder.id
    }

    override suspend fun claimDueOccurrence(id: Long): Reminder? = withContext(dispatcher) {
        val existing = reminderDao.get(id)?.toDomain() ?: return@withContext null
        val occurrence = existing.dueAtEpochMs ?: return@withContext null
        if (existing.completed) return@withContext null
        // One atomic UPDATE decides the winner; a zero row count means someone else already
        // announced this occurrence and we must not post a second copy of it.
        if (reminderDao.claimOccurrence(id, occurrence) == 0) return@withContext null
        existing.copy(lastFiredOccurrenceEpochMs = occurrence)
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) = withContext(dispatcher) {
        val existing = reminderDao.get(id)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()

        // Completing one occurrence of a repeating reminder shouldn't retire the series —
        // it advances to the next due time and stays open, like Samsung Reminder does.
        val next = if (completed) existing.nextOccurrence(now) else null
        val updated = if (next != null) {
            existing.copy(dueAtEpochMs = next, completed = false, completedAtEpochMs = null)
        } else {
            existing.copy(
                completed = completed,
                completedAtEpochMs = if (completed) now else null,
                // Re-opening a reminder gives its current occurrence another chance to be
                // announced; without dropping the claim it would sit there permanently silent.
                lastFiredOccurrenceEpochMs = if (completed) {
                    existing.lastFiredOccurrenceEpochMs
                } else {
                    null
                },
            )
        }

        reminderDao.update(updated.toEntity())
        notifications.cancel(id)
        scheduler.schedule(updated)
    }

    /**
     * Roll a repeating reminder onto its next occurrence and re-arm it, leaving anything already
     * on screen alone.
     *
     * Separate from [save] precisely because of that last part: this runs *while* the current
     * occurrence is being shown to the user, and [save] cancels the reminder's notification on
     * the assumption that an edit supersedes it. Advancing the series must not take down the
     * overlay the user is looking at.
     */
    override suspend fun advanceRepeating(id: Long): Unit = withContext(dispatcher) {
        val existing = reminderDao.get(id)?.toDomain() ?: return@withContext
        val next = existing.nextOccurrence() ?: return@withContext
        val rolled = existing.copy(
            dueAtEpochMs = next,
            completed = false,
            completedAtEpochMs = null,
            // The claim belonged to the occurrence just delivered; the new one is unclaimed.
            lastFiredOccurrenceEpochMs = null,
        )
        reminderDao.update(rolled.toEntity())
        scheduler.schedule(rolled)
    }

    override suspend fun snooze(id: Long, minutes: Int) = withContext(dispatcher) {
        val existing = reminderDao.get(id)?.toDomain() ?: return@withContext
        val updated = existing.copy(
            dueAtEpochMs = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L,
            completed = false,
            completedAtEpochMs = null,
            // A snooze creates a fresh obligation at a new time, so it starts unclaimed. The
            // old claim must not linger, or the snoozed reminder would never announce itself.
            lastFiredOccurrenceEpochMs = null,
        )
        reminderDao.update(updated.toEntity())
        notifications.cancel(id)
        scheduler.schedule(updated)
    }

    override suspend fun delete(id: Long) = withContext(dispatcher) {
        scheduler.cancel(id)
        notifications.cancel(id)
        reminderDao.deleteById(id)
    }

    override suspend fun deleteCompleted() = withContext(dispatcher) {
        // Completed reminders hold no live alarms, so the rows can just go.
        reminderDao.deleteCompleted()
    }

    /**
     * Re-arm every pending alarm. Safe to call as often as you like — this is the sweep that
     * runs after a reboot, after an app update, and whenever the launcher comes back up.
     *
     * The previous version of this method was the single biggest source of repeated reminders:
     * an overdue one-shot was re-notified on *every* sweep, so one missed reminder produced a
     * fresh notification after every reboot and every app update, forever. Now the overdue case
     * goes through the same atomic claim as a real alarm, so it surfaces at most once and then
     * stays quiet.
     */
    override suspend fun rescheduleAll() = withContext(dispatcher) {
        val now = System.currentTimeMillis()
        reminderDao.getPendingScheduled().forEach { entity ->
            val reminder = entity.toDomain()
            val due = reminder.dueAtEpochMs ?: return@forEach

            if (due > now) {
                // Still in the future: schedule() cancels first, so re-running this cannot
                // leave two alarms behind for the same reminder.
                scheduler.schedule(reminder)
                return@forEach
            }

            val next = reminder.nextOccurrence(now)
            if (next != null) {
                // Repeating and missed while the device was off. Roll forward silently rather
                // than firing once for every occurrence that went by.
                val rolled = reminder.copy(
                    dueAtEpochMs = next,
                    lastFiredOccurrenceEpochMs = null,
                )
                reminderDao.update(rolled.toEntity())
                scheduler.schedule(rolled)
                return@forEach
            }

            // One-shot and overdue. Claim it: the first sweep after the missed time surfaces
            // it, and every sweep after that finds the claim taken and says nothing.
            if (reminderDao.claimOccurrence(reminder.id, due) > 0) {
                notifications.notifyDue(reminder)
            }
        }
    }

    private fun ReminderEntity.toDomain() = Reminder(
        id = id,
        title = title,
        note = note,
        dueAtEpochMs = dueAtEpochMs,
        repeatRule = runCatching { RepeatRule.valueOf(repeatRule) }.getOrDefault(RepeatRule.NONE),
        completed = completed,
        completedAtEpochMs = completedAtEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        lastFiredOccurrenceEpochMs = lastFiredOccurrenceEpochMs,
    )

    private fun Reminder.toEntity() = ReminderEntity(
        id = id,
        title = title,
        note = note,
        dueAtEpochMs = dueAtEpochMs,
        repeatRule = repeatRule.name,
        completed = completed,
        completedAtEpochMs = completedAtEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        lastFiredOccurrenceEpochMs = lastFiredOccurrenceEpochMs,
    )
}
