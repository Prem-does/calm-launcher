package com.calmlauncher.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.calmlauncher.R
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.feature.reminderalert.ReminderAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Minutes the notification's quick-snooze action pushes a reminder out by. */
const val REMINDER_SNOOZE_MINUTES = 10

/**
 * The notification half of reminder delivery.
 *
 * Since reminders became a full-screen interruption, notifications play two narrower roles rather
 * than being the main event:
 *
 *  - [notifyDueFullScreen] carries the full-screen intent that lets a background alarm broadcast
 *    legally take the screen. Its visible form is a fallback for when the platform declines to
 *    honour that intent.
 *  - [notifyDue] with `silent = true` is a *record* posted alongside a successful overlay, so a
 *    reminder isn't lost if that overlay is torn down by the system.
 *
 * Both actions are handled by [ReminderAlarmReceiver] without opening the launcher, so dealing
 * with a reminder never pulls the user into the phone.
 */
@Singleton
class ReminderNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: CalmNotifications,
) {

    /**
     * Post the reminder as an ordinary notification.
     *
     * @param silent posts into the low-importance channel with no sound or vibration. Used when a
     *   louder surface has already interrupted the user — a second buzz for the same reminder
     *   reads as exactly the duplicate bug this system was built to eliminate.
     */
    fun notifyDue(reminder: Reminder, silent: Boolean = false) {
        val channel = if (silent) CalmChannels.REMINDERS_QUIET else CalmChannels.REMINDERS
        val builder = baseBuilder(reminder, channel)
            .setPriority(
                if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH,
            )
            .setSilent(silent)
        notifications.post(notificationId(reminder.id), builder.build())
    }

    /**
     * Post the reminder with a full-screen intent pointing at [ReminderAlertActivity].
     *
     * @param expectHonoured whether the platform is expected to act on the full-screen intent. It
     *   only changes the notification's *visible* treatment: when the intent will be honoured the
     *   notification is a silent placeholder behind the Activity, and when it won't, the
     *   notification is all the user is going to get, so it goes out loud and heads-up.
     * @return true if the notification was posted at all (false when notifications are blocked).
     */
    fun notifyDueFullScreen(reminder: Reminder, expectHonoured: Boolean): Boolean {
        if (!notifications.canPost()) return false
        val builder = baseBuilder(reminder, CalmChannels.REMINDERS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(expectHonoured)
            .setFullScreenIntent(alertActivityIntent(reminder.id), true)
        notifications.post(notificationId(reminder.id), builder.build())
        return true
    }

    fun cancel(reminderId: Long) {
        notifications.cancel(notificationId(reminderId))
    }

    private fun baseBuilder(reminder: Reminder, channelId: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            // Tapping the body opens the full interruption, not the launcher home screen — the
            // reminder is the thing the user is being asked about.
            .setContentIntent(alertActivityIntent(reminder.id))
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
        return builder
    }

    /** A PendingIntent that brings up the full-screen reminder overlay for [reminderId]. */
    private fun alertActivityIntent(reminderId: Long): PendingIntent? = runCatching {
        PendingIntent.getActivity(
            context,
            ALERT_REQUEST_BASE + reminderId.toInt(),
            ReminderAlertActivity.intent(context, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

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

    private companion object {
        /** Reserved band for the full-screen alert PendingIntents. */
        const val ALERT_REQUEST_BASE = 700_000
    }
}
