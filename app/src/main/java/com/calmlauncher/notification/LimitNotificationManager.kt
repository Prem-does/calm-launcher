package com.calmlauncher.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LimitNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val channelId = "calm_launcher_app_limits"

    init {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(channelId, "App limits", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications about apps approaching their daily limits"
            },
        )
    }

    fun notifyApproachingLimit(packageName: String, label: String, remainingMinutes: Int) {
        val id = packageName.hashCode()
        val title = "$label — time remaining"
        val text = when (remainingMinutes) {
            1 -> "1 minute until daily limit"
            0 -> "Limit reached"
            else -> "$remainingMinutes minutes until daily limit"
        }
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
