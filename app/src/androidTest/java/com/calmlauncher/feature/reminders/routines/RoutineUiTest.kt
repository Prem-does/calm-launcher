package com.calmlauncher.feature.reminders.routines

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineTaskType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RoutineUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun checkboxTaskTogglesThroughCallback() {
        var checked = false
        composeRule.setContent {
            RoutineTaskRow(
                item = RoutineTaskUiState(
                    routineId = 1L,
                    routineTitle = "Morning",
                    taskId = 42L,
                    title = "Water",
                    taskType = RoutineTaskType.CHECKBOX,
                    targetValue = null,
                    unit = "",
                    reminderMinuteOfDay = null,
                    completed = false,
                    actualValue = null,
                    completionFraction = 0f,
                ),
                onCheckboxChanged = { checked = it },
                onMetricChanged = {},
            )
        }

        composeRule.onNodeWithTag("routine_task_checkbox_42").performClick()
        assertEquals(true, checked)
    }

    @Test
    fun metricTaskAcceptsNumericInput() {
        var metric: Int? = null
        composeRule.setContent {
            RoutineTaskRow(
                item = RoutineTaskUiState(
                    routineId = 1L,
                    routineTitle = "Morning",
                    taskId = 43L,
                    title = "Protein",
                    taskType = RoutineTaskType.METRIC,
                    targetValue = 140,
                    unit = "g",
                    reminderMinuteOfDay = null,
                    completed = false,
                    actualValue = null,
                    completionFraction = 0f,
                ),
                onCheckboxChanged = {},
                onMetricChanged = { metric = it },
            )
        }

        composeRule.onNodeWithTag("routine_task_metric_43").performTextInput("112")
        assertEquals(112, metric)
    }

    @Test
    fun routineBuilderSavesNewRoutine() {
        var saved: Routine? = null
        composeRule.setContent {
            RoutineBuilderDialog(
                routine = null,
                onDismiss = {},
                onSave = { saved = it },
            )
        }

        composeRule.onNodeWithTag("routine_builder_title").performTextInput("Morning")
        composeRule.onNodeWithText("SAVE").assertIsDisplayed().performClick()

        assertEquals("Morning", saved?.title)
        assertEquals(1, saved?.tasks?.size)
    }
}