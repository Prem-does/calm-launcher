package com.calmlauncher.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.calmlauncher.R
import com.calmlauncher.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Minutes a snooze action pushes a reminder out by. */
const val REMINDER_SNOOZE_MINUTES = 10

/**
 * Posts a due reminder with Samsung-Reminder-style actions: mark it done, or snooze it for
 * [REMINDER_SNOOZE_MINUTES]. Both actions are handled by [ReminderAlarmReceiver] without
 * opening the launcher, so acting on a reminder never pulls the user into the phone.
 */
@Singleton
class ReminderNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: CalmNotifications,
) {

    fun notifyDue(reminder: Reminder) {
        val id = notificationId(reminder.id)
        val builder = NotificationCompat.Builder(context, CalmChannels.REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentIntent(notifications.openLauncherIntent(id))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(
                0,
                "Done",
                actionIntent(reminder.id, ReminderAlarmReceiver.ACTION_COMPLETE),
            )
            .addAction(
                0,
                "Snooze ${REMINDER_SNOOZE_MINUTES}m",
                actionIntent(reminder.id, ReminderAlarmReceiver.ACTION_SNOOZE),
            )

        if (reminder.note.isNotBlank()) {
            builder.setContentText(reminder.note)
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(reminder.note))
        } else {
            builder.setContentText("Reminder")
        }

        notifications.post(id, builder.build())
    }

    fun cancel(reminderId: Long) {
        notifications.cancel(notificationId(reminderId))
    }

    private fun actionIntent(reminderId: Long, action: String): PendingIntent? = runCatching {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        PendingIntent.getBroadcast(
            context,
            // Distinct request code per (reminder, action) so the two actions don't collide.
            (action.hashCode() * 31 + reminderId.toInt()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    /** Offset keeps reminder notification ids clear of the package-hash-based limit ids. */
    private fun notificationId(reminderId: Long): Int = 500_000 + reminderId.toInt()
}
