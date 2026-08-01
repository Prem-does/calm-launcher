package com.calmlauncher.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.calmlauncher.core.util.formatReminderDueTime
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.feature.reminderalert.ReminderAlertActivity
import com.calmlauncher.feature.reminderalert.SnoozeChoices
import com.calmlauncher.overlay.ReminderOverlayController
import com.calmlauncher.overlay.ReminderOverlaySpec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides *how* a due reminder interrupts the user, and makes sure it always does.
 *
 * There is no single way to take the screen from a background alarm on modern Android, so this
 * tries the available routes in order of how hard they are to ignore, stopping at the first that
 * works:
 *
 *  1. **A window overlay** ([ReminderOverlayController]), when "display over other apps" is
 *     granted. Composited above the foreground app by definition — no race to lose, and no
 *     dependence on a permission Android 14 hands out sparingly.
 *  2. **The full-screen Activity** ([ReminderAlertActivity]), reached via a full-screen-intent
 *     notification and a direct `startActivity`. Works on the lock screen and needs no optional
 *     grant. The notification is what legitimises the launch from a broadcast receiver.
 *  3. **A plain notification with actions** ([ReminderNotificationManager]). Weakest, but a
 *     reminder that can't be shown loudly must still be shown.
 *
 * The ordering is a judgement about which failure is worse. An overlay that appears when the
 * screen is off is useless, so if the device is idle the Activity route goes first — it can turn
 * the screen on, and the overlay cannot.
 *
 * Everything is best-effort and wrapped: this runs inside an alarm broadcast, where an exception
 * costs the user the reminder entirely.
 */
@Singleton
class ReminderAlertPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlay: ReminderOverlayController,
    private val notifications: ReminderNotificationManager,
) {

    /**
     * Interrupt the user with [reminder]. Callers must already have claimed the occurrence via
     * [com.calmlauncher.domain.repository.ReminderRepository.claimDueOccurrence] — this method
     * does no duplicate checking of its own, and calling it twice for one occurrence would show
     * the reminder twice.
     *
     * [onSnooze] and [onFinished] are only used by the overlay route; the Activity and the
     * notification carry their own handlers.
     */
    fun present(
        reminder: Reminder,
        onSnooze: (minutes: Int) -> Unit,
        onFinished: () -> Unit,
    ) {
        // A quiet notification always goes up first as the record of what happened. It is
        // silenced (and re-posted as an ongoing summary) when a louder route succeeds, so the
        // reminder is recoverable if the user dismisses the overlay's parent task.
        if (tryActivityFirst()) {
            if (startActivity(reminder)) return
            if (showOverlay(reminder, onSnooze, onFinished)) return
        } else {
            if (showOverlay(reminder, onSnooze, onFinished)) return
            if (startActivity(reminder)) return
        }
        notifications.notifyDue(reminder)
    }

    /** Take down anything currently showing for [reminderId]. */
    fun dismiss(reminderId: Long) {
        if (overlay.showingReminderId() == reminderId) overlay.hide()
        notifications.cancel(reminderId)
    }

    /**
     * True when the Activity route should be tried first — that is, when the screen is off or the
     * device is dozing, where only the Activity can wake the display.
     */
    private fun tryActivityFirst(): Boolean = runCatching {
        val pm = context.getSystemService(android.os.PowerManager::class.java) ?: return false
        !pm.isInteractive
    }.getOrDefault(false)

    /**
     * Post a full-screen-intent notification and also attempt a direct launch.
     *
     * Both, not either: the full-screen intent is the route the platform sanctions from a
     * background broadcast, but Android 14+ only honours it with `USE_FULL_SCREEN_INTENT`
     * granted, in which case it silently degrades to a heads-up notification. The direct
     * `startActivity` covers the other side — it succeeds when the launcher is already in the
     * foreground, or when "display over other apps" is held, which exempts the app from
     * background-activity-launch restrictions.
     *
     * Returns true only when at least one route plausibly took the screen.
     */
    private fun startActivity(reminder: Reminder): Boolean {
        val fullScreenPosted = notifications.notifyDueFullScreen(reminder, canUseFullScreenIntent())
        val directLaunched = runCatching {
            context.startActivity(ReminderAlertActivity.intent(context, reminder.id))
            true
        }.getOrDefault(false)
        return directLaunched || (fullScreenPosted && canUseFullScreenIntent())
    }

    private fun showOverlay(
        reminder: Reminder,
        onSnooze: (Int) -> Unit,
        onFinished: () -> Unit,
    ): Boolean {
        if (!overlay.canShow()) return false
        val shown = overlay.show(
            spec = ReminderOverlaySpec(
                reminderId = reminder.id,
                title = reminder.title,
                note = reminder.note,
                dueLabel = reminder.dueAtEpochMs?.let { formatReminderDueTime(it) },
                snoozeChoices = SnoozeChoices,
            ),
            onSnooze = onSnooze,
            onFinished = onFinished,
        )
        if (shown) {
            // Keep a silent copy in the shade so the reminder survives the overlay being torn
            // down by the system (a low-memory kill, or the permission being revoked live).
            notifications.notifyDue(reminder, silent = true)
        }
        return shown
    }

    /**
     * Whether a full-screen intent will actually take the screen. Android 14 restricts the
     * permission to calling and alarm apps unless the user grants it explicitly; below that it is
     * a normal install-time permission.
     */
    private fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return runCatching {
            context.getSystemService(NotificationManager::class.java)
                ?.canUseFullScreenIntent() == true
        }.getOrDefault(false)
    }
}
