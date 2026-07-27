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
        val id = if (reminder.id == 0L) {
            reminderDao.insert(reminder.copy(createdAtEpochMs = now).toEntity())
        } else {
            reminderDao.update(reminder.toEntity())
            reminder.id
        }
        scheduler.schedule(reminder.copy(id = id))
        id
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
            )
        }

        reminderDao.update(updated.toEntity())
        notifications.cancel(id)
        scheduler.schedule(updated)
    }

    override suspend fun snooze(id: Long, minutes: Int) = withContext(dispatcher) {
        val existing = reminderDao.get(id)?.toDomain() ?: return@withContext
        val updated = existing.copy(
            dueAtEpochMs = System.currentTimeMillis() + minutes * 60_000L,
            completed = false,
            completedAtEpochMs = null,
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

    override suspend fun rescheduleAll() = withContext(dispatcher) {
        reminderDao.getPendingScheduled().forEach { entity ->
            val reminder = entity.toDomain()
            // A reminder whose time passed while the device was off has no future alarm to
            // set; roll repeating ones forward so they resume on their next occurrence.
            val due = reminder.dueAtEpochMs ?: return@forEach
            if (due > System.currentTimeMillis()) {
                scheduler.schedule(reminder)
            } else {
                val next = reminder.nextOccurrence()
                if (next != null) {
                    val rolled = reminder.copy(dueAtEpochMs = next)
                    reminderDao.update(rolled.toEntity())
                    scheduler.schedule(rolled)
                } else {
                    // One-shot and overdue: surface it now rather than losing it silently.
                    notifications.notifyDue(reminder)
                }
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
    )
}
