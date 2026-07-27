package com.calmlauncher.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.calmlauncher.launcher.LauncherActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification channel ids. Splitting by intent (rather than one catch-all channel) is what
 * lets the user silence limit warnings while keeping reminder alarms audible — Android only
 * exposes per-channel controls, so a single channel means all-or-nothing.
 */
object CalmChannels {
    /** Heads-up-free "you're approaching a limit" nudges. Silent by design. */
    const val LIMIT_WARNING = "calm_limit_warning"

    /** "You've hit the limit" — the one moment worth an audible cue. */
    const val LIMIT_REACHED = "calm_limit_reached"

    /** Reminders and tasks that are due now. */
    const val REMINDERS = "calm_reminders"
}

/**
 * Creates the launcher's notification channels once and answers whether posting is even
 * possible. Every notification manager in the app goes through here so channel definitions
 * live in exactly one place and callers never post into a channel that doesn't exist yet.
 */
@Singleton
class CalmNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    init {
        ensureChannels()
    }

    /**
     * True when the user has notifications enabled for the app. On Android 13+ this covers a
     * denied POST_NOTIFICATIONS permission too, so callers can bail out silently instead of
     * posting into the void.
     */
    fun canPost(): Boolean = runCatching {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }.getOrDefault(false)

    /** A PendingIntent that brings the launcher forward — every notification is tappable. */
    fun openLauncherIntent(requestCode: Int = 0): PendingIntent? = runCatching {
        val intent = Intent(context, LauncherActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    /** Post [notification] under [id], swallowing the SecurityException on a denied permission. */
    fun post(id: Int, notification: Notification) {
        if (!canPost()) return
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    fun cancel(id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }

    private fun ensureChannels() {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            nm.createNotificationChannel(
                NotificationChannel(
                    CalmChannels.LIMIT_WARNING,
                    "App limit warnings",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Quiet heads-up as an app approaches its daily limit."
                    setShowBadge(false)
                    enableVibration(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CalmChannels.LIMIT_REACHED,
                    "App limit reached",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Told once when an app has used up its daily limit."
                    setShowBadge(true)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CalmChannels.REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Your reminders and tasks, when they're due."
                    setShowBadge(true)
                    enableVibration(true)
                },
            )
            // Retire the pre-split channel so it stops appearing in system settings.
            nm.deleteNotificationChannel(LEGACY_CHANNEL)
        }
    }

    private companion object {
        const val LEGACY_CHANNEL = "calm_launcher_app_limits"
    }
}
