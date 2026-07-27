package com.calmlauncher.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.calmlauncher.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-limit notifications. Two distinct shapes, because they mean different things:
 *
 *  - **Approaching** is a silent, low-importance nudge. It replaces itself as the countdown
 *    ticks (10m → 5m → 1m) rather than stacking up three separate notifications.
 *  - **Reached** is the one moment worth interrupting for, and it's told exactly once.
 *
 * Both are tappable straight into the launcher, and both are cancelled by [clear] when the
 * limit stops applying (rule disabled/removed, override granted, or a new day rolls over).
 */
@Singleton
class LimitNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: CalmNotifications,
) {

    fun notifyApproachingLimit(packageName: String, label: String, remainingMinutes: Int) {
        if (remainingMinutes <= 0) {
            notifyLimitReached(packageName, label)
            return
        }
        val text = if (remainingMinutes == 1) {
            "1 minute left today"
        } else {
            "$remainingMinutes minutes left today"
        }
        val notification = NotificationCompat.Builder(context, CalmChannels.LIMIT_WARNING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$text. When it runs out, $label stops opening until tomorrow.",
                ),
            )
            .setContentIntent(notifications.openLauncherIntent(warningId(packageName)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        notifications.post(warningId(packageName), notification)
    }

    fun notifyLimitReached(packageName: String, label: String) {
        // The countdown is over — drop the warning so both don't sit in the shade together.
        notifications.cancel(warningId(packageName))
        val notification = NotificationCompat.Builder(context, CalmChannels.LIMIT_REACHED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$label is done for today")
            .setContentText("Daily limit reached.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Daily limit reached. You can grant yourself a short extension from App Limits if you really need it.",
                ),
            )
            .setContentIntent(notifications.openLauncherIntent(reachedId(packageName)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        notifications.post(reachedId(packageName), notification)
    }

    /** Remove both notifications for a package — the limit no longer applies. */
    fun clear(packageName: String) {
        notifications.cancel(warningId(packageName))
        notifications.cancel(reachedId(packageName))
    }

    private fun warningId(packageName: String): Int = packageName.hashCode()

    private fun reachedId(packageName: String): Int = packageName.hashCode() + 1
}
