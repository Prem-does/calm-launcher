package com.calmlauncher.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.calmlauncher.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the AlarmManager entries behind reminders. A reminder is only ever scheduled when it
 * is open and has a due time in the future; anything else is cancelled, which keeps a stale
 * alarm from firing for a reminder the user has already completed or deleted.
 *
 * Exact alarms are used where permitted so a 09:00 reminder arrives at 09:00. Where the
 * platform withholds that permission we fall back to an inexact alarm rather than dropping
 * the reminder entirely.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Arm (or re-arm) [reminder]'s single alarm, cancelling whatever was there first.
     *
     * Cancelling before setting is not redundant with `FLAG_UPDATE_CURRENT`. The update flag
     * only replaces the *extras* of a matching PendingIntent; the cancel is what guarantees no
     * previously-armed trigger time for this reminder survives, which is the difference between
     * one alarm per reminder and a slowly accumulating pile of them across edits and snoozes.
     *
     * An occurrence that has already been announced is deliberately left unarmed: re-arming it
     * would schedule an alarm whose only possible outcome is a claim that fails, and on a
     * one-shot reminder that alarm would otherwise fire on every reschedule sweep.
     */
    fun schedule(reminder: Reminder) {
        cancel(reminder.id)

        val dueAt = reminder.dueAtEpochMs
        if (reminder.completed || dueAt == null) return
        if (reminder.currentOccurrenceAlreadyFired) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = pendingIntent(reminder.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return

        runCatching {
            if (canScheduleExact(am)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dueAt, pendingIntent)
            }
        }
    }

    fun cancel(reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // NO_CREATE returns null when nothing is scheduled, which is a no-op cancel.
        val pendingIntent = pendingIntent(reminderId, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching {
            am.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun pendingIntent(reminderId: Long, extraFlags: Int): PendingIntent? = runCatching {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_FIRE
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        PendingIntent.getBroadcast(
            context,
            requestCode(reminderId),
            intent,
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    /**
     * Stable per-reminder request code. Ids are Long and request codes are Int, so the id is
     * folded into a reserved positive band rather than truncated: a bare `toInt()` can go
     * negative on a large id and land on top of an app-limit alarm's hash-derived code, which
     * would let cancelling a reminder silently disarm a limit warning (or vice versa).
     */
    private fun requestCode(reminderId: Long): Int =
        REQUEST_CODE_BASE + ((reminderId xor (reminderId ushr 32)).toInt() and REQUEST_CODE_MASK)

    private fun canScheduleExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    private companion object {
        /** Start of the band reserved for reminder alarms. */
        const val REQUEST_CODE_BASE = 100_000

        /** Keeps the folded id positive and inside the reserved band (~1M codes). */
        const val REQUEST_CODE_MASK = 0x0F_FFFF
    }
}
