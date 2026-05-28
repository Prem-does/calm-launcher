package com.calmlauncher.accessibility

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.calmlauncher.launcher.hideNotifications
import com.calmlauncher.launcher.recoveryMode
import com.calmlauncher.launcher.silentNotifications
import com.calmlauncher.launcher.hideNotificationIcons
import com.calmlauncher.launcher.disableNotificationBadges
import com.calmlauncher.launcher.disablePopups
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.calmlauncher.data.db.CalmDatabaseProvider
import com.calmlauncher.data.db.entity.TelemetryEvent
import kotlinx.coroutines.GlobalScope


class CalmNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        serviceScope.launch {
            val settings = PlatformGuardPolicy.settings(this@CalmNotificationListenerService)
            val shouldSuppress = settings.hideNotifications() || settings.silentNotifications() || settings.recoveryMode() || settings.hideNotificationIcons() || settings.disableNotificationBadges() || settings.disablePopups()
            if (shouldSuppress) {
                // Cancel the notification to prevent popups, icons, badges, sounds.
                cancelNotification(key)
                // Log telemetry about the suppressed notification
                try {
                    val db = CalmDatabaseProvider.get(this@CalmNotificationListenerService)
                    GlobalScope.launch(Dispatchers.IO) {
                        val pkg = sbn.packageName ?: "unknown"
                        val title = sbn.notification.extras?.getString("android.title") ?: ""
                        db.telemetryDao().insert(TelemetryEvent(type = "notification_suppressed", details = "$pkg|$title", timestampMillis = System.currentTimeMillis()))
                    }
                } catch (_: Throwable) { }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
