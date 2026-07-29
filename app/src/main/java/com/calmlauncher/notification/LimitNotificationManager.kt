package com.calmlauncher.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.calmlauncher.R
import com.calmlauncher.domain.model.LimitNotifyStage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-limit notifications: **one live notification per app, ever**.
 *
 * A single id per package means the countdown replaces itself in place (10m → 5m → 1m →
 * done) instead of stacking four separate entries in the shade. Warnings are silent and
 * low-importance; the single "limit reached" message is the one moment worth an audible cue,
 * and it is posted at most once a day.
 *
 * This class only knows *how* to render a stage. Deciding **whether** a stage is new — and
 * therefore worth posting at all — belongs to
 * [com.calmlauncher.data.repository.AppLimitRepositoryImpl], which owns the recorded
 * [LimitNotifyStage] for each rule. Both the exact alarm and the usage rollup go through that
 * one path, which is what stops the two of them double-posting.
 */
@Singleton
class LimitNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: CalmNotifications,
) {

    /**
     * Render [stage] for [packageName], replacing whatever that app was showing before.
     *
     * [LimitNotifyStage.NONE] means "nothing to say", and clears instead of posting.
     */
    fun notifyStage(
        packageName: String,
        label: String,
        stage: LimitNotifyStage,
        remainingMinutes: Int,
    ) {
        when (stage) {
            LimitNotifyStage.NONE -> clear(packageName)
            LimitNotifyStage.REACHED -> notifyLimitReached(packageName, label)
            else -> notifyApproachingLimit(packageName, label, remainingMinutes.coerceAtLeast(1))
        }
    }

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
                    "$text. When it runs out, $label closes itself until tomorrow.",
                ),
            )
            .setContentIntent(notifications.openLauncherIntent(notificationId(packageName)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        notifications.post(notificationId(packageName), notification)
    }

    fun notifyLimitReached(packageName: String, label: String) {
        // Moving from the silent warning channel to the audible one: cancel first, because an
        // in-place update would keep the notification on its original channel and stay silent.
        notifications.cancel(notificationId(packageName))
        val notification = NotificationCompat.Builder(context, CalmChannels.LIMIT_REACHED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$label is done for today")
            .setContentText("Daily limit reached.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Daily limit reached. You can grant yourself a short extension from App Limits if you really need it.",
                ),
            )
            .setContentIntent(notifications.openLauncherIntent(notificationId(packageName)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        notifications.post(notificationId(packageName), notification)
    }

    /** Remove this app's notification — the limit no longer applies. */
    fun clear(packageName: String) {
        notifications.cancel(notificationId(packageName))
        // Builds before the single-notification rework posted "reached" under a second id;
        // retire any that survived the upgrade.
        notifications.cancel(notificationId(packageName) + 1)
    }

    private fun notificationId(packageName: String): Int = packageName.hashCode()
}
