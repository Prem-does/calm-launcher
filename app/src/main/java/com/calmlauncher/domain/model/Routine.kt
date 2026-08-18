package com.calmlauncher.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class RoutineTaskType { CHECKBOX, METRIC }

data class RoutineTask(
    val id: Long = 0L,
    val routineId: Long = 0L,
    val title: String,
    val taskType: RoutineTaskType = RoutineTaskType.CHECKBOX,
    val targetValue: Int? = null,
    val unit: String = "",
    val reminderMinuteOfDay: Int? = null,
    val orderIndex: Int = 0,
)

data class RoutineCompletion(
    val taskId: Long,
    val dayStartEpochMs: Long,
    val completed: Boolean,
    val actualValue: Int? = null,
    val timestampEpochMs: Long,
)

data class Routine(
    val id: Long = 0L,
    val title: String,
    val activeDaysMask: Int,
    val createdAtEpochMs: Long = 0L,
    val tasks: List<RoutineTask> = emptyList(),
) {
    fun isActiveOn(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val dayOfWeek = LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), zoneId).dayOfWeek
        return activeDaysMask.hasDay(dayOfWeek)
    }
}

fun dayStartEpochMs(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()

fun currentWeekStartEpochMs(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val date = LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), zoneId)
    val monday = date.minusDays(((date.dayOfWeek.value + 6) % 7).toLong())
    return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun completionFraction(task: RoutineTask, completion: RoutineCompletion?): Float = when (task.taskType) {
    RoutineTaskType.CHECKBOX -> if (completion?.completed == true) 1f else 0f
    RoutineTaskType.METRIC -> {
        val target = (task.targetValue ?: 0).coerceAtLeast(1)
        val actual = completion?.actualValue ?: 0
        (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    }
}

fun routineProgressFraction(tasks: List<RoutineTask>, completions: Map<Long, RoutineCompletion?>): Float {
    if (tasks.isEmpty()) return 0f
    val total = tasks.size.toFloat().coerceAtLeast(1f)
    val progress = tasks.sumOf { task -> completionFraction(task, completions[task.id]).toDouble() }
    return (progress / total).toFloat().coerceIn(0f, 1f)
}

/**
 * How many consecutive active days up to today have been completed in full.
 *
 * A rest day — one with no routine scheduled — is skipped rather than counted or treated as a
 * break, which is exactly why the walk backwards has to be bounded explicitly. Without
 * [maxLookbackDays], a user whose routines are all inactive — or who has no routines at all,
 * which is every user the first time they open this screen — never reaches the exit condition
 * and this walks back through epoch time forever, hanging whichever thread called it.
 */
fun routineStreak(
    dayProgress: Map<Long, Float>,
    dayIsActive: (Long) -> Boolean,
    todayStartEpochMs: Long,
    maxLookbackDays: Int = ROUTINE_STREAK_LOOKBACK_DAYS,
): Int {
    var streak = 0
    var cursor = todayStartEpochMs
    repeat(maxLookbackDays) {
        if (dayIsActive(cursor)) {
            if ((dayProgress[cursor] ?: 0f) < 1f) return streak
            streak += 1
        }
        cursor -= DAY_MILLIS
    }
    return streak
}

fun Int.hasDay(dayOfWeek: DayOfWeek): Boolean {
    val bit = when (dayOfWeek) {
        DayOfWeek.MONDAY -> 1 shl 0
        DayOfWeek.TUESDAY -> 1 shl 1
        DayOfWeek.WEDNESDAY -> 1 shl 2
        DayOfWeek.THURSDAY -> 1 shl 3
        DayOfWeek.FRIDAY -> 1 shl 4
        DayOfWeek.SATURDAY -> 1 shl 5
        DayOfWeek.SUNDAY -> 1 shl 6
    }
    return (this and bit) != 0
}

fun weekdaysMask(): Int =
    (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)

fun allDaysMask(): Int = (1 shl 7) - 1

private const val DAY_MILLIS = 86_400_000L

/**
 * How far back a streak walk and the dashboard progress history both look.
 *
 * Shared so the two cannot drift: the streak may only count days the dashboard has actually
 * computed progress for.
 */
const val ROUTINE_STREAK_LOOKBACK_DAYS = 90
