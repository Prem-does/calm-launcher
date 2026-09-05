package com.calmlauncher.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineTask
import com.calmlauncher.domain.model.hasDay
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun schedule(routine: Routine, task: RoutineTask) {
        if (!routine.enabled) {
            cancel(task.id)
            return
        }
        val reminderMinute = task.reminderMinuteOfDay ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = nextOccurrence(routine, reminderMinute) ?: return
        cancel(task.id)
        val pendingIntent = pendingIntent(routine.id, task.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancel(taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = pendingIntent(0L, taskId, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching {
            am.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAll(routines: List<Routine>) {
        routines.forEach { routine ->
            routine.tasks.forEach { task -> schedule(routine, task) }
        }
    }

    private fun nextOccurrence(routine: Routine, minuteOfDay: Int): Long? {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val start = now.toLocalDate()
        repeat(8) { offset ->
            val day = start.plusDays(offset.toLong())
            if (routine.activeDaysMask.hasDay(day.dayOfWeek)) {
                val candidate = day.atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
                if (candidate.isAfter(now)) {
                    return candidate.atZone(zone).toInstant().toEpochMilli()
                }
            }
        }
        return null
    }

    private fun pendingIntent(routineId: Long, taskId: Long, extraFlags: Int): PendingIntent? = runCatching {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = RoutineAlarmReceiver.ACTION_FIRE
            putExtra(RoutineAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(RoutineAlarmReceiver.EXTRA_ROUTINE_ID, routineId)
        }
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )
    }.getOrNull()

    private fun requestCode(taskId: Long): Int =
        REQUEST_CODE_BASE + ((taskId xor (taskId ushr 32)).toInt() and REQUEST_CODE_MASK)

    private companion object {
        const val REQUEST_CODE_BASE = 300_000
        const val REQUEST_CODE_MASK = 0x0F_FFFF
    }
}