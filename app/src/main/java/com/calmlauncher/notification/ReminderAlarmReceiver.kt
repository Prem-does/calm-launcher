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
 *  - [ACTION_FIRE] — the alarm went off: claim the occurrence, interrupt the user, and roll a
 *    repeating reminder forward so the following one is already armed.
 *  - [ACTION_COMPLETE] / [ACTION_SNOOZE] — notification and overlay actions.
 *  - `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` — Android drops every alarm across a restart or an
 *    update, so re-arm them all.
 *
 * **The claim is the whole design.** Every one of those triggers can legitimately arrive for a
 * reminder that has already been shown — a redundant alarm delivery after the device leaves doze,
 * a boot sweep racing an alarm that survived, an update landing seconds after one fired. None of
 * them can see what the others did, so rather than trying to coordinate them, [fire] asks the
 * database for permission through a single atomic UPDATE. Exactly one caller wins and the rest do
 * nothing, which is what makes "a reminder appears once per scheduled event" true rather than
 * merely likely.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var presenter: ReminderAlertPresenter

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

                    ACTION_SNOOZE -> snooze(reminderId, REMINDER_SNOOZE_MINUTES)
                }
            } catch (_: Exception) {
                // A reminder failure must never crash the receiver.
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * The alarm fired. Claim the occurrence first and bail out silently when the claim fails —
     * that means either a duplicate delivery, or the reminder was dealt with in the gap between
     * the alarm being set and it going off.
     */
    private suspend fun fire(reminderId: Long) {
        if (reminderId < 0) return
        val reminder = reminderRepository.claimDueOccurrence(reminderId) ?: return

        // Roll a repeating reminder forward *before* showing this one, so the series keeps running
        // even if the process is killed while the overlay is up. advanceRepeating deliberately
        // leaves notifications alone, so this can't take down what we're about to show.
        if (reminder.repeatRule != RepeatRule.NONE) {
            reminderRepository.advanceRepeating(reminderId)
        }

        presenter.present(
            reminder = reminder,
            onSnooze = { minutes -> scope.launch { snooze(reminderId, minutes) } },
            onFinished = { scope.launch { complete(reminderId) } },
        )
    }

    /**
     * "Done", from the notification or the overlay. A repeating reminder was already advanced to
     * its next occurrence when it fired, so completing it here must only clear what is on screen —
     * calling through to the repository would advance it a second time and skip a day.
     */
    private suspend fun complete(reminderId: Long) {
        if (reminderId < 0) return
        presenter.dismiss(reminderId)
        val reminder = reminderRepository.get(reminderId) ?: return
        if (reminder.repeatRule == RepeatRule.NONE) {
            reminderRepository.setCompleted(reminderId, true)
        }
    }

    /**
     * Snooze. The repository moves the due time and clears the delivery claim in one write, so the
     * snoozed occurrence is armed exactly once — no path here can leave two alarms pending for the
     * same reminder.
     */
    private suspend fun snooze(reminderId: Long, minutes: Int) {
        if (reminderId < 0) return
        presenter.dismiss(reminderId)
        reminderRepository.snooze(reminderId, minutes)
    }

    companion object {
        const val ACTION_FIRE = "com.calmlauncher.action.REMINDER_FIRE"
        const val ACTION_COMPLETE = "com.calmlauncher.action.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.calmlauncher.action.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminderId"
    }
}
