package com.calmlauncher.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.calmlauncher.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: CalmNotifications,
) {
    fun notifyDue(taskId: Long, routineTitle: String, taskTitle: String) {
        val notification = NotificationCompat.Builder(context, CalmChannels.ROUTINES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(taskTitle)
            .setContentText(routineTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$routineTitle · $taskTitle"))
            .setContentIntent(notifications.openLauncherIntent(notificationId(taskId)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .addAction(0, "Done", actionIntent(taskId, RoutineAlarmReceiver.ACTION_COMPLETE))
            .build()
        notifications.post(notificationId(taskId), notification)
    }

    fun cancel(taskId: Long) {
        notifications.cancel(notificationId(taskId))
    }

    private fun actionIntent(taskId: Long, action: String): PendingIntent? = runCatching {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(RoutineAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(RoutineAlarmReceiver.EXTRA_ROUTINE_ID, 0L)
        }
        PendingIntent.getBroadcast(
            context,
            (action.hashCode() * 31 + taskId.toInt()),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    private fun notificationId(taskId: Long): Int = 600_000 + taskId.toInt()
}