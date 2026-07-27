package com.calmlauncher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.domain.model.RepeatRule
import com.calmlauncher.domain.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles everything that happens to a reminder outside the UI:
 *
 *  - [ACTION_FIRE] — the alarm went off: post the notification and, if the reminder repeats,
 *    roll it forward to its next occurrence so the following one is already armed.
 *  - [ACTION_COMPLETE] / [ACTION_SNOOZE] — notification actions.
 *  - `BOOT_COMPLETED` — Android drops every alarm across a reboot, so re-arm them all.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var notifications: ReminderNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val pending = goAsync()

        scope.launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    -> reminderRepository.rescheduleAll()

                    ACTION_FIRE -> fire(reminderId)

                    ACTION_COMPLETE -> complete(reminderId)

                    ACTION_SNOOZE -> {
                        if (reminderId >= 0) {
                            notifications.cancel(reminderId)
                            reminderRepository.snooze(reminderId, REMINDER_SNOOZE_MINUTES)
                        }
                    }
                }
            } catch (_: Exception) {
                // A reminder failure must never crash the receiver.
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(reminderId: Long) {
        if (reminderId < 0) return
        val reminder = reminderRepository.get(reminderId) ?: return
        // Completed in the gap between the alarm being set and it firing — stay quiet.
        if (reminder.completed) return

        notifications.notifyDue(reminder)

        // Repeating reminders roll forward as soon as they fire, so the series keeps running
        // whether or not the user ever acts on this notification. Saving re-arms the alarm.
        val next = reminder.nextOccurrence()
        if (next != null) {
            reminderRepository.save(reminder.copy(dueAtEpochMs = next))
        }
    }

    /**
     * "Done" from the notification. A repeating reminder was already advanced to its next
     * occurrence when it fired, so completing it here must only dismiss the notification —
     * calling through to the repository would advance it a second time and skip a day.
     */
    private suspend fun complete(reminderId: Long) {
        if (reminderId < 0) return
        notifications.cancel(reminderId)
        val reminder = reminderRepository.get(reminderId) ?: return
        if (reminder.repeatRule == RepeatRule.NONE) {
            reminderRepository.setCompleted(reminderId, true)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.calmlauncher.action.REMINDER_FIRE"
        const val ACTION_COMPLETE = "com.calmlauncher.action.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.calmlauncher.action.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminderId"
    }
}
