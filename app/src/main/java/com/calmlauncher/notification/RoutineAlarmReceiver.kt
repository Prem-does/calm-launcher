package com.calmlauncher.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calmlauncher.domain.model.RoutineCompletion
import com.calmlauncher.domain.repository.RoutineRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RoutineAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var scheduler: RoutineScheduler
    @Inject lateinit var notifications: RoutineNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED -> routineRepository.refreshRollover(System.currentTimeMillis())
                    ACTION_FIRE -> fire(routineId, taskId)
                    ACTION_COMPLETE -> complete(taskId)
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(routineId: Long, taskId: Long) {
        if (taskId < 0 || routineId < 0) return
        val routine = routineRepository.getRoutine(routineId) ?: return
        val task = routine.tasks.first { it.id == taskId }
        notifications.notifyDue(taskId, routine.title, task.title)
        scheduler.schedule(routine, task)
    }

    private suspend fun complete(taskId: Long) {
        if (taskId < 0) return
        notifications.cancel(taskId)
        val now = System.currentTimeMillis()
        val dayStart = com.calmlauncher.domain.model.dayStartEpochMs(now)
        routineRepository.setTaskCompletion(
            taskId = taskId,
            dayStartEpochMs = dayStart,
            completion = RoutineCompletion(
                taskId = taskId,
                dayStartEpochMs = dayStart,
                completed = true,
                actualValue = null,
                timestampEpochMs = now,
            ),
        )
    }

    companion object {
        const val ACTION_FIRE = "com.calmlauncher.action.ROUTINE_FIRE"
        const val ACTION_COMPLETE = "com.calmlauncher.action.ROUTINE_COMPLETE"
        const val EXTRA_TASK_ID = "routineTaskId"
        const val EXTRA_ROUTINE_ID = "routineId"
    }
}