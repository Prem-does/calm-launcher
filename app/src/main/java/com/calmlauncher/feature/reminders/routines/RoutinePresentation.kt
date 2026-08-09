package com.calmlauncher.feature.reminders.routines

import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineCompletion
import com.calmlauncher.domain.model.RoutineTask
import com.calmlauncher.domain.model.RoutineTaskType
import com.calmlauncher.domain.model.allDaysMask
import com.calmlauncher.domain.model.currentWeekStartEpochMs
import com.calmlauncher.domain.model.dayStartEpochMs
import com.calmlauncher.domain.model.hasDay
import com.calmlauncher.domain.model.routineProgressFraction
import com.calmlauncher.domain.model.routineStreak
import com.calmlauncher.domain.model.weekdaysMask
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class RoutineTaskUiState(
    val routineId: Long,
    val routineTitle: String,
    val taskId: Long,
    val title: String,
    val taskType: RoutineTaskType,
    val targetValue: Int?,
    val unit: String,
    val reminderMinuteOfDay: Int?,
    val completed: Boolean,
    val actualValue: Int?,
    val completionFraction: Float,
)

data class RoutineDayProgressUiState(
    val dayStartEpochMs: Long,
    val progress: Float,
)

data class RoutineDashboardUiState(
    val todayProgress: Float = 0f,
    val weeklyProgress: List<RoutineDayProgressUiState> = emptyList(),
    val streakDays: Int = 0,
    val todayTasks: List<RoutineTaskUiState> = emptyList(),
    val routines: List<Routine> = emptyList(),
) {
    val hasRoutines: Boolean get() = routines.isNotEmpty()
}

fun buildRoutineDashboard(
    routines: List<Routine>,
    completions: List<RoutineCompletion>,
    nowEpochMs: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): RoutineDashboardUiState {
    val todayStart = dayStartEpochMs(nowEpochMs, zoneId)
    val completionsByDay = completions.groupBy { it.dayStartEpochMs }
        .mapValues { (_, rows) -> rows.associateBy { it.taskId } }
    val activeToday = routines.filter { it.isActiveOn(todayStart, zoneId) }
    val todayTasks = activeToday.flatMap { routine ->
        val dayCompletions = completionsByDay[todayStart].orEmpty()
        routine.tasks.map { task ->
            val completion = dayCompletions[task.id]
            RoutineTaskUiState(
                routineId = routine.id,
                routineTitle = routine.title,
                taskId = task.id,
                title = task.title,
                taskType = task.taskType,
                targetValue = task.targetValue,
                unit = task.unit,
                reminderMinuteOfDay = task.reminderMinuteOfDay,
                completed = completion?.completed == true,
                actualValue = completion?.actualValue,
                completionFraction = taskCompletionFraction(task, completion),
            )
        }
    }

    val weekStart = currentWeekStartEpochMs(nowEpochMs, zoneId)
    val weeklyProgress = (0 until 7).map { offset ->
        val dayStart = weekStart + offset * DAY_MILLIS
        RoutineDayProgressUiState(
            dayStartEpochMs = dayStart,
            progress = routineProgressForDay(routines, completionsByDay[dayStart].orEmpty(), dayStart, zoneId),
        )
    }

    val progressByDay = generateSequence(todayStart) { it - DAY_MILLIS }
        .take(90)
        .associateWith { dayStart ->
            routineProgressForDay(routines, completionsByDay[dayStart].orEmpty(), dayStart, zoneId)
        }
    val streakDays = routineStreak(
        dayProgress = progressByDay,
        dayIsActive = { day -> routines.any { it.isActiveOn(day, zoneId) && it.tasks.isNotEmpty() } },
        todayStartEpochMs = todayStart,
    )

    return RoutineDashboardUiState(
        todayProgress = routineProgressForDay(routines, completionsByDay[todayStart].orEmpty(), todayStart, zoneId),
        weeklyProgress = weeklyProgress,
        streakDays = streakDays,
        todayTasks = todayTasks,
        routines = routines,
    )
}

fun routineProgressForDay(
    routines: List<Routine>,
    completions: Map<Long, RoutineCompletion?>,
    dayStartEpochMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Float {
    val activeTasks = routines
        .filter { it.isActiveOn(dayStartEpochMs, zoneId) }
        .flatMap { it.tasks }
    if (activeTasks.isEmpty()) return 0f
    val relevantCompletions = activeTasks.associate { task -> task.id to completions[task.id] }
    return routineProgressFraction(activeTasks, relevantCompletions)
}

fun taskCompletionFraction(task: RoutineTask, completion: RoutineCompletion?): Float = when (task.taskType) {
    RoutineTaskType.CHECKBOX -> if (completion?.completed == true) 1f else 0f
    RoutineTaskType.METRIC -> {
        val target = (task.targetValue ?: 0).coerceAtLeast(1)
        val actual = completion?.actualValue ?: 0
        (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }
}

fun routineDaysLabel(activeDaysMask: Int): String {
    return when (activeDaysMask) {
        allDaysMask() -> "Everyday"
        weekdaysMask() -> "Weekdays"
        0 -> "None"
        else -> buildString {
            DayOfWeek.entries.forEach { day ->
                if (activeDaysMask.hasDay(day)) {
                    if (isNotEmpty()) append(", ")
                    append(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

private const val DAY_MILLIS = 86_400_000L