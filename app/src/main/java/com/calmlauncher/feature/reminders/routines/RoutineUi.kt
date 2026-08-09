package com.calmlauncher.feature.reminders.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.Routine
import com.calmlauncher.domain.model.RoutineTask
import com.calmlauncher.domain.model.RoutineTaskType
import com.calmlauncher.domain.model.allDaysMask
import com.calmlauncher.domain.model.hasDay
import com.calmlauncher.domain.model.weekdaysMask
import java.time.DayOfWeek

@Composable
fun CalmProgressGauge(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(220.dp, 160.dp)) {
                val strokeWidth = 18.dp.toPx()
                val startAngle = 180f
                val sweep = 180f
                drawArc(
                    color = CalmGrayDim,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = CalmWhite,
                    startAngle = startAngle,
                    sweepAngle = (sweep * progress.coerceIn(0f, 1f)).coerceAtLeast(1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                val needleAngle = Math.toRadians((180f + 180f * progress.coerceIn(0f, 1f)).toDouble())
                val center = Offset(size.width / 2f, size.height)
                val radius = size.width / 2.5f
                drawLine(
                    color = CalmWhite,
                    start = center,
                    end = Offset(
                        x = center.x + (kotlin.math.cos(needleAngle) * radius).toFloat(),
                        y = center.y + (kotlin.math.sin(needleAngle) * radius).toFloat(),
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            Text(text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%", style = CalmType.headlineLgMobile, color = CalmWhite)
        }
    }
}

@Composable
fun WeeklyCompletionStrip(
    days: List<RoutineDayProgressUiState>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        days.forEach { day ->
            val fraction = day.progress.coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .background(CalmGrayDim)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(10.dp)
                            .background(CalmWhite),
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineTaskRow(
    item: RoutineTaskUiState,
    onCheckboxChanged: (Boolean) -> Unit,
    onMetricChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.routineTitle, style = CalmType.labelMd, color = CalmGray)
                Text(text = item.title, style = CalmType.bodyLg, color = CalmWhite)
                if (item.taskType == RoutineTaskType.METRIC) {
                    Text(
                        text = metricLabel(item),
                        style = CalmType.labelMd,
                        color = CalmGray,
                        modifier = Modifier.padding(top = Spacing.stackSm),
                    )
                }
            }
            when (item.taskType) {
                RoutineTaskType.CHECKBOX -> CheckMark(
                    taskId = item.taskId,
                    checked = item.completed,
                    onCheckedChange = onCheckboxChanged,
                )
                RoutineTaskType.METRIC -> MetricField(item = item, onMetricChanged = onMetricChanged)
            }
        }
        ThinDivider()
    }
}

@Composable
private fun CheckMark(taskId: Long, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(18.dp)
            .testTag("routine_task_checkbox_$taskId")
            .border(1.dp, if (checked) CalmWhite else CalmGray)
            .background(if (checked) CalmWhite else CalmBlack)
            .clickable(interactionSource = interaction, indication = null, onClick = { onCheckedChange(!checked) }),
    )
}

private fun metricLabel(item: RoutineTaskUiState): String = buildString {
    append(item.actualValue?.toString() ?: "0")
    item.targetValue?.let { target ->
        append(" / ")
        append(target)
    }
    if (item.unit.isNotBlank()) {
        append(" ")
        append(item.unit)
    }
}

@Composable
fun RoutineDashboardSection(
    dashboard: RoutineDashboardUiState,
    onNewRoutine: () -> Unit,
    onEditRoutine: (Routine) -> Unit,
    onCheckboxChanged: (Long, Boolean) -> Unit,
    onMetricChanged: (Long, Int?) -> Unit,
) {
    SectionLabel("Routines")
    CalmProgressGauge(progress = dashboard.todayProgress)
    WeeklyCompletionStrip(
        days = dashboard.weeklyProgress,
        modifier = Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackMd),
    )
    SettingRow(title = "Day streak", value = "${dashboard.streakDays} day streak")
    SettingRow(title = "New routine", onClick = onNewRoutine, showChevron = true)

    dashboard.routines.forEach { routine ->
        SettingRow(
            title = routine.title,
            value = routineDaysLabel(routine.activeDaysMask),
            onClick = { onEditRoutine(routine) },
            showChevron = true,
        )
    }

    if (dashboard.todayTasks.isNotEmpty()) {
        SectionLabel("Today")
        dashboard.todayTasks.forEach { task ->
            RoutineTaskRow(
                item = task,
                onCheckboxChanged = { checked -> onCheckboxChanged(task.taskId, checked) },
                onMetricChanged = { value -> onMetricChanged(task.taskId, value) },
            )
        }
    } else {
        Text(
            text = "No routine tasks for today.",
            style = CalmType.labelMd,
            color = CalmGray,
            modifier = Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        )
        ThinDivider()
    }
}

@Composable
private fun MetricField(
    item: RoutineTaskUiState,
    onMetricChanged: (Int?) -> Unit,
) {
    var value by remember(item.taskId, item.actualValue) { mutableStateOf(item.actualValue?.toString().orEmpty()) }
    BasicTextField(
        value = value,
        onValueChange = {
            value = it.filter { ch -> ch.isDigit() }
            onMetricChanged(value.toIntOrNull())
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = CalmType.bodyLg.copy(color = CalmWhite),
        modifier = Modifier.width(84.dp).testTag("routine_task_metric_${item.taskId}"),
    )
}

@Composable
fun RoutineBuilderDialog(
    routine: Routine?,
    onDismiss: () -> Unit,
    onSave: (Routine) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val initial = routine ?: Routine(title = "", activeDaysMask = weekdaysMask(), tasks = emptyList())
    var title by remember(initial.id) { mutableStateOf(initial.title) }
    var activeDaysMask by remember(initial.id) { mutableStateOf(initial.activeDaysMask) }
    var tasks by remember(initial.id) {
        mutableStateOf<List<RoutineDraftTaskState>>(initial.tasks.map { it.toDraft() }.ifEmpty { listOf(defaultTask()) })
    }

    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CalmBlack)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.stackLg),
        ) {
            CalmBackBar(title = if (routine == null || routine.id == 0L) "New routine" else "Edit routine", onBack = onDismiss)
            TextFieldLine(value = title, onValueChange = { title = it }, placeholder = "Routine title")
            SectionLabel("Days")
            RoutinePresetRow(
                activeDaysMask = activeDaysMask,
                onPreset = { activeDaysMask = it },
            )
            RoutineDayToggleRow(
                activeDaysMask = activeDaysMask,
                onToggleDay = { day -> activeDaysMask = activeDaysMask xor day.mask }
            )
            SectionLabel("Tasks")
            tasks.forEachIndexed { index, task ->
                RoutineTaskEditorRow(
                    task = task,
                    onChange = { updated -> tasks = tasks.toMutableList().also { it[index] = updated } },
                    onDelete = if (tasks.size > 1) {
                        { tasks = tasks.filterIndexed { taskIndex, _ -> taskIndex != index } }
                    } else null,
                    onMoveUp = if (index > 0) {
                        {
                            val mutable = tasks.toMutableList()
                            val current = mutable[index]
                            mutable[index] = mutable[index - 1]
                            mutable[index - 1] = current
                            tasks = mutable
                        }
                    } else null,
                    onMoveDown = if (index < tasks.lastIndex) {
                        {
                            val mutable = tasks.toMutableList()
                            val current = mutable[index]
                            mutable[index] = mutable[index + 1]
                            mutable[index + 1] = current
                            tasks = mutable
                        }
                    } else null,
                )
            }
            SettingRow(title = "Add task", onClick = { tasks = tasks + defaultTask() }, showChevron = true)
            Row(modifier = Modifier.fillMaxWidth().padding(Spacing.marginMobile), horizontalArrangement = Arrangement.spacedBy(Spacing.gutter)) {
                CalmButton(text = "SAVE", style = CalmButtonStyle.Filled, enabled = title.isNotBlank(), onClick = {
                    onSave(
                        Routine(
                            id = routine?.id ?: 0L,
                            title = title,
                            activeDaysMask = activeDaysMask,
                            createdAtEpochMs = routine?.createdAtEpochMs ?: System.currentTimeMillis(),
                            tasks = tasks.mapIndexed { index, task ->
                                task.toDomain(index, routine?.id ?: 0L)
                            },
                        ),
                    )
                })
                if (onDelete != null) {
                    CalmButton(text = "DELETE", style = CalmButtonStyle.Text, onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun RoutinePresetRow(activeDaysMask: Int, onPreset: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        RoutinePresetButton("Weekdays", selected = activeDaysMask == weekdaysMask(), onClick = { onPreset(weekdaysMask()) })
        RoutinePresetButton("Everyday", selected = activeDaysMask == allDaysMask(), onClick = { onPreset(allDaysMask()) })
        RoutinePresetButton("Custom", selected = activeDaysMask != weekdaysMask() && activeDaysMask != allDaysMask(), onClick = { })
    }
    ThinDivider()
}

@Composable
private fun RoutineDayToggleRow(activeDaysMask: Int, onToggleDay: (DayOfWeek) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        DayOfWeek.entries.forEach { day ->
            RoutinePresetButton(
                text = day.name.take(3),
                selected = activeDaysMask.hasDay(day),
                onClick = { onToggleDay(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    ThinDivider()
}

private val DayOfWeek.mask: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1 shl 0
        DayOfWeek.TUESDAY -> 1 shl 1
        DayOfWeek.WEDNESDAY -> 1 shl 2
        DayOfWeek.THURSDAY -> 1 shl 3
        DayOfWeek.FRIDAY -> 1 shl 4
        DayOfWeek.SATURDAY -> 1 shl 5
        DayOfWeek.SUNDAY -> 1 shl 6
    }

@Composable
private fun RoutinePresetButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CalmButton(
        text = text,
        style = if (selected) CalmButtonStyle.Filled else CalmButtonStyle.Outlined,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun RoutineTaskEditorRow(
    task: RoutineDraftTaskState,
    onChange: (RoutineDraftTaskState) -> Unit,
    onDelete: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    Column {
        TextFieldLine(value = task.title, onValueChange = { onChange(task.copy(title = it)) }, placeholder = "Task title")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile), horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
            RoutinePresetButton("Checkbox", selected = task.taskType == RoutineTaskType.CHECKBOX, onClick = { onChange(task.copy(taskType = RoutineTaskType.CHECKBOX)) }, modifier = Modifier.weight(1f))
            RoutinePresetButton("Metric", selected = task.taskType == RoutineTaskType.METRIC, onClick = { onChange(task.copy(taskType = RoutineTaskType.METRIC)) }, modifier = Modifier.weight(1f))
        }
        if (task.taskType == RoutineTaskType.METRIC) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile), horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
                SmallTextField(value = task.targetValue, placeholder = "Target", onValueChange = { onChange(task.copy(targetValue = it)) }, modifier = Modifier.weight(1f))
                SmallTextField(value = task.unit, placeholder = "Unit", onValueChange = { onChange(task.copy(unit = it)) }, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile), horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
            onMoveUp?.let { CalmButton(text = "Up", style = CalmButtonStyle.Text, onClick = it, modifier = Modifier.weight(1f)) }
            onMoveDown?.let { CalmButton(text = "Down", style = CalmButtonStyle.Text, onClick = it, modifier = Modifier.weight(1f)) }
            onDelete?.let { CalmButton(text = "Remove", style = CalmButtonStyle.Text, onClick = it, modifier = Modifier.weight(1f)) }
        }
        ThinDivider()
    }
}

@Composable
private fun TextFieldLine(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = CalmType.bodyLg.copy(color = CalmWhite),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(CalmWhite),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("routine_builder_title")
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(text = placeholder, style = CalmType.bodyLg, color = CalmGray)
                }
                inner()
            },
        )
        ThinDivider()
    }
}

@Composable
private fun SmallTextField(value: String, placeholder: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = CalmType.bodyMd.copy(color = CalmWhite),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(CalmWhite),
        singleLine = true,
        modifier = modifier.border(1.dp, CalmGray).padding(horizontal = Spacing.base, vertical = Spacing.stackSm),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(text = placeholder, style = CalmType.bodyMd, color = CalmGray)
            }
            inner()
        },
    )
}

private data class RoutineDraftTaskState(
    val id: Long = 0L,
    val title: String = "",
    val taskType: RoutineTaskType = RoutineTaskType.CHECKBOX,
    val targetValue: String = "",
    val unit: String = "",
    val reminderMinuteOfDay: String = "",
)

private fun defaultTask() = RoutineDraftTaskState()

private fun RoutineTask.toDraft() = RoutineDraftTaskState(
    id = id,
    title = title,
    taskType = taskType,
    targetValue = targetValue?.toString().orEmpty(),
    unit = unit,
    reminderMinuteOfDay = reminderMinuteOfDay?.toString().orEmpty(),
)

private fun RoutineDraftTaskState.toDomain(orderIndex: Int, routineId: Long) = RoutineTask(
    id = id,
    routineId = routineId,
    title = title,
    taskType = taskType,
    targetValue = targetValue.toIntOrNull(),
    unit = unit,
    reminderMinuteOfDay = reminderMinuteOfDay.toIntOrNull(),
    orderIndex = orderIndex,
)

private data class RoutineDraftTaskStateHolder(val state: RoutineDraftTaskState)