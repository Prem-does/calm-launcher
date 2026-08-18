package com.calmlauncher.feature.reminders.routines

import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineCompletion
import com.calmlauncher.domain.model.RoutineTask
import com.calmlauncher.domain.model.RoutineTaskType
import com.calmlauncher.domain.model.allDaysMask
import com.calmlauncher.domain.model.dayStartEpochMs
import com.calmlauncher.domain.model.routineStreak
import com.calmlauncher.domain.model.weekdaysMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutinePresentationTest {

    @Test
    fun checkboxAndMetricProgressAreAveragedTogether() {
        val routine = Routine(
            id = 1L,
            title = "Morning",
            activeDaysMask = allDaysMask(),
            tasks = listOf(
                RoutineTask(id = 10L, routineId = 1L, title = "Water", taskType = RoutineTaskType.CHECKBOX),
                RoutineTask(id = 11L, routineId = 1L, title = "Protein", taskType = RoutineTaskType.METRIC, targetValue = 100, unit = "g"),
            ),
        )
        val now = System.currentTimeMillis()
        val today = dayStartEpochMs(now)
        val dashboard = buildRoutineDashboard(
            routines = listOf(routine),
            completions = listOf(
                RoutineCompletion(taskId = 10L, dayStartEpochMs = today, completed = true, actualValue = null, timestampEpochMs = now),
                RoutineCompletion(taskId = 11L, dayStartEpochMs = today, completed = false, actualValue = 50, timestampEpochMs = now),
            ),
            nowEpochMs = now,
        )

        assertEquals(0.75f, dashboard.todayProgress, 0.0001f)
        assertEquals(1, dashboard.todayTasks.count { it.completed })
    }

    @Test
    fun streakCountsOnlyConsecutiveFullDays() {
        val today = dayStartEpochMs(System.currentTimeMillis())
        val progress = mapOf(
            today to 1f,
            today - DAY_MILLIS to 1f,
            today - 2 * DAY_MILLIS to 0f,
        )
        val streak = routineStreak(
            dayProgress = progress,
            dayIsActive = { true },
            todayStartEpochMs = today,
        )

        assertEquals(2, streak)
    }

    @Test
    fun yesterdayCompletionDoesNotCountForToday() {
        val routine = Routine(
            id = 1L,
            title = "Routine",
            activeDaysMask = weekdaysMask(),
            tasks = listOf(RoutineTask(id = 10L, routineId = 1L, title = "Task")),
        )
        val now = System.currentTimeMillis()
        val today = dayStartEpochMs(now)
        val yesterday = today - DAY_MILLIS
        val dashboard = buildRoutineDashboard(
            routines = listOf(routine),
            completions = listOf(
                RoutineCompletion(taskId = 10L, dayStartEpochMs = yesterday, completed = true, actualValue = null, timestampEpochMs = now),
            ),
            nowEpochMs = now,
        )

        assertTrue(dashboard.todayProgress == 0f)
        assertEquals(0, dashboard.streakDays)
    }

    /**
     * The freeze that made the to-do tab unopenable: with no routines, `dayIsActive` is false for
     * every day, so the streak walk had no exit condition and span backwards through epoch time on
     * the main thread. The timeout is the assertion — a reintroduced unbounded walk fails here
     * instead of hanging the suite.
     */
    @Test(timeout = 2_000L)
    fun streakTerminatesWhenNoDayIsEverActive() {
        val today = dayStartEpochMs(System.currentTimeMillis())

        val streak = routineStreak(
            dayProgress = emptyMap(),
            dayIsActive = { false },
            todayStartEpochMs = today,
        )

        assertEquals(0, streak)
    }

    @Test(timeout = 2_000L)
    fun emptyDashboardBuildsWithoutHanging() {
        val dashboard = buildRoutineDashboard(
            routines = emptyList(),
            completions = emptyList(),
            nowEpochMs = System.currentTimeMillis(),
        )

        assertEquals(0, dashboard.streakDays)
        assertTrue(dashboard.todayTasks.isEmpty())
    }

    /** A day with nothing scheduled is skipped, not counted and not treated as a break. */
    @Test(timeout = 2_000L)
    fun restDaysAreSkippedRatherThanBreakingTheStreak() {
        val today = dayStartEpochMs(System.currentTimeMillis())
        val restDay = today - DAY_MILLIS
        val progress = mapOf(
            today to 1f,
            today - 2 * DAY_MILLIS to 1f,
        )

        val streak = routineStreak(
            dayProgress = progress,
            dayIsActive = { day -> day != restDay },
            todayStartEpochMs = today,
        )

        assertEquals(2, streak)
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000L
    }
}