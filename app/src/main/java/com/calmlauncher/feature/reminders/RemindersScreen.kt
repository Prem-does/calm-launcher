package com.calmlauncher.feature.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.Reminder
import com.calmlauncher.domain.model.RepeatRule
import java.util.Calendar
import java.util.Locale

private const val SnoozeMinutes = 10

/**
 * Reminders and tasks in the launcher's own idiom: a flat black list split into overdue,
 * upcoming and done, with no cards, borders or chips. Each row is a check mark, a title and
 * one grey meta line; tapping opens the editor, long-pressing deletes.
 */
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Reminders", onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = Spacing.stackLg),
        ) {
            item {
                SettingRow(
                    title = "New reminder",
                    onClick = { editing = Reminder(title = "") },
                    showChevron = true,
                )
            }

            if (state.isEmpty) {
                item {
                    Text(
                        text = "Nothing to remember yet.",
                        style = CalmType.bodyMd,
                        color = CalmGray,
                        modifier = Modifier.padding(
                            horizontal = Spacing.marginMobile,
                            vertical = Spacing.rowVertical,
                        ),
                    )
                }
            }

            reminderSection(
                label = "Due now",
                reminders = state.overdue,
                onToggleComplete = viewModel::setCompleted,
                onEdit = { editing = it },
                onLongPress = { pendingDelete = it },
                onSnooze = { viewModel.snooze(it, SnoozeMinutes) },
            )
            reminderSection(
                label = "Upcoming",
                reminders = state.upcoming,
                onToggleComplete = viewModel::setCompleted,
                onEdit = { editing = it },
                onLongPress = { pendingDelete = it },
                onSnooze = null,
            )
            reminderSection(
                label = "Done",
                reminders = state.completed,
                onToggleComplete = viewModel::setCompleted,
                onEdit = { editing = it },
                onLongPress = { pendingDelete = it },
                onSnooze = null,
            )

            if (state.completed.isNotEmpty()) {
                item {
                    SettingRow(title = "Clear done", onClick = viewModel::clearCompleted)
                }
            }
        }
    }

    editing?.let { reminder ->
        ReminderEditor(
            reminder = reminder,
            onDismiss = { editing = null },
            onSave = { title, note, dueAt, repeat ->
                viewModel.save(reminder.id, title, note, dueAt, repeat)
                editing = null
            },
            onDelete = if (reminder.id == 0L) {
                null
            } else {
                {
                    viewModel.delete(reminder)
                    editing = null
                }
            },
        )
    }

    pendingDelete?.let { reminder ->
        ConfirmSheet(
            title = "Delete reminder?",
            detail = reminder.title,
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.delete(reminder)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** Emits a labelled group of rows, or nothing at all when the group is empty. */
private fun androidx.compose.foundation.lazy.LazyListScope.reminderSection(
    label: String,
    reminders: List<Reminder>,
    onToggleComplete: (Reminder, Boolean) -> Unit,
    onEdit: (Reminder) -> Unit,
    onLongPress: (Reminder) -> Unit,
    onSnooze: ((Reminder) -> Unit)?,
) {
    if (reminders.isEmpty()) return
    item { SectionLabel(label) }
    items(reminders, key = { it.id }) { reminder ->
        ReminderRow(
            reminder = reminder,
            onToggleComplete = { onToggleComplete(reminder, it) },
            onEdit = { onEdit(reminder) },
            onLongPress = { onLongPress(reminder) },
            onSnooze = onSnooze?.let { snooze -> { snooze(reminder) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(
    reminder: Reminder,
    onToggleComplete: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
    onSnooze: (() -> Unit)?,
) {
    val interaction = remember { MutableInteractionSource() }
    val titleColor = if (reminder.completed) CalmGrayDim else CalmWhite

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onEdit,
                    onLongClick = onLongPress,
                )
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            CheckMark(checked = reminder.completed, onCheckedChange = onToggleComplete)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = CalmType.bodyLg,
                    color = titleColor,
                    maxLines = 2,
                )
                Text(
                    text = metaLine(reminder),
                    style = CalmType.labelMd,
                    color = if (reminder.isOverdue()) CalmWhite else CalmGray,
                    maxLines = 1,
                )
            }
            if (onSnooze != null) {
                Text(
                    text = "SNOOZE",
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSnooze,
                    ),
                )
            }
        }
        ThinDivider()
    }
}

/** A 1dp square that fills solid when checked. The launcher has no use for a Material checkbox. */
@Composable
private fun CheckMark(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(1.dp, if (checked) CalmWhite else CalmGray)
            .background(if (checked) CalmWhite else CalmBlack)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
    )
}

/** "Today 09:00 · Weekly" — due time and repeat on one grey line, or "No date". */
private fun metaLine(reminder: Reminder): String {
    val due = reminder.dueAtEpochMs?.let { formatDueDateTime(it) } ?: "No date"
    return if (reminder.repeatRule == RepeatRule.NONE) {
        due
    } else {
        "$due · ${reminder.repeatRule.label()}"
    }
}

/**
 * The reminder editor as a full black screen rather than a Material dialog: underlined
 * fields for the title and note, then plain rows for date, time and repeat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditor(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onSave: (title: String, note: String, dueAtEpochMs: Long?, repeat: RepeatRule) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(reminder.id) { mutableStateOf(reminder.title) }
    var note by remember(reminder.id) { mutableStateOf(reminder.note) }
    var dueAt by remember(reminder.id) { mutableStateOf(reminder.dueAtEpochMs) }
    var repeat by remember(reminder.id) { mutableStateOf(reminder.repeatRule) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CalmBlack)
                // CalmBackBar applies the status-bar inset itself; only the bottom is left.
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            CalmBackBar(
                title = if (reminder.id == 0L) "New reminder" else "Edit reminder",
                onBack = onDismiss,
            )

            UnderlinedField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Title",
                imeAction = ImeAction.Next,
            )
            UnderlinedField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Note",
                imeAction = ImeAction.Done,
                singleLine = false,
            )

            SectionLabel("When")
            SettingRow(
                title = "Date",
                value = dueAt?.let { formatDate(it) } ?: "None",
                onClick = { showDatePicker = true },
            )
            SettingRow(
                title = "Time",
                value = dueAt?.let { formatTime(it) } ?: "—",
                onClick = { if (dueAt != null) showTimePicker = true },
            )
            SettingRow(
                title = "Repeat",
                value = repeat.label(),
                // A repeat has nothing to repeat from without a due time.
                onClick = { if (dueAt != null) repeat = repeat.next() },
            )
            if (dueAt != null) {
                SettingRow(
                    title = "Clear date",
                    onClick = {
                        dueAt = null
                        repeat = RepeatRule.NONE
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.marginMobile),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            ) {
                CalmButton(
                    text = "SAVE",
                    style = CalmButtonStyle.Filled,
                    enabled = title.isNotBlank(),
                    onClick = { onSave(title, note, dueAt, repeat) },
                )
                if (onDelete != null) {
                    CalmButton(text = "DELETE", style = CalmButtonStyle.Text, onClick = onDelete)
                }
            }

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueAt ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                CalmButton(
                    text = "OK",
                    style = CalmButtonStyle.Text,
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            // The picker returns UTC midnight; keep whatever time-of-day the
                            // reminder already had (or default to 09:00) on the chosen date.
                            dueAt = combineDateWithExistingTime(picked, dueAt)
                        }
                        showDatePicker = false
                    },
                )
            },
            dismissButton = {
                CalmButton(
                    text = "Cancel",
                    style = CalmButtonStyle.Text,
                    onClick = { showDatePicker = false },
                )
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val existing = Calendar.getInstance().apply { timeInMillis = dueAt ?: System.currentTimeMillis() }
        val timePickerState = rememberTimePickerState(
            initialHour = existing.get(Calendar.HOUR_OF_DAY),
            initialMinute = existing.get(Calendar.MINUTE),
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .background(CalmBlack)
                    .padding(Spacing.marginMobile),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timePickerState)
                Row(
                    modifier = Modifier.padding(top = Spacing.stackMd),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                ) {
                    CalmButton(
                        text = "Cancel",
                        style = CalmButtonStyle.Text,
                        onClick = { showTimePicker = false },
                    )
                    CalmButton(
                        text = "OK",
                        style = CalmButtonStyle.Outlined,
                        onClick = {
                            dueAt = withTimeOfDay(
                                dueAt ?: System.currentTimeMillis(),
                                timePickerState.hour,
                                timePickerState.minute,
                            )
                            showTimePicker = false
                        },
                    )
                }
            }
        }
    }
}

/** A bare text field: white text on black with a single grey rule underneath. */
@Composable
private fun UnderlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    imeAction: ImeAction,
    singleLine: Boolean = true,
) {
    Column {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = CalmType.bodyLg.copy(color = CalmWhite),
            cursorBrush = SolidColor(CalmWhite),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            modifier = Modifier
                .fillMaxWidth()
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

/** A full-bleed yes/no, styled like the rest of the launcher instead of a Material alert. */
@Composable
private fun ConfirmSheet(
    title: String,
    detail: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CalmBlack),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(horizontal = Spacing.marginMobile),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = title, style = CalmType.headlineMd, color = CalmWhite)
                Text(
                    text = detail,
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.stackLg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                ) {
                    CalmButton(text = "Keep", style = CalmButtonStyle.Outlined, onClick = onDismiss)
                    CalmButton(text = confirmLabel, style = CalmButtonStyle.Filled, onClick = onConfirm)
                }
            }
        }
    }
}

private fun RepeatRule.label(): String = when (this) {
    RepeatRule.NONE -> "Never"
    RepeatRule.DAILY -> "Daily"
    RepeatRule.WEEKLY -> "Weekly"
    RepeatRule.MONTHLY -> "Monthly"
    RepeatRule.YEARLY -> "Yearly"
}

private fun RepeatRule.next(): RepeatRule {
    val values = RepeatRule.entries
    return values[(ordinal + 1) % values.size]
}

/**
 * [DatePicker] hands back UTC midnight for the chosen day. Read the calendar fields in UTC
 * and apply them to a local-time calendar, otherwise timezones east/west of UTC shift the
 * reminder onto the wrong day.
 */
private fun combineDateWithExistingTime(pickedUtcMidnight: Long, existingDueAt: Long?): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = pickedUtcMidnight
    }
    val existing = Calendar.getInstance().apply {
        timeInMillis = existingDueAt ?: System.currentTimeMillis()
    }
    val hour = if (existingDueAt != null) existing.get(Calendar.HOUR_OF_DAY) else DEFAULT_HOUR
    val minute = if (existingDueAt != null) existing.get(Calendar.MINUTE) else 0

    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun withTimeOfDay(baseEpochMs: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = baseEpochMs
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** "Today 09:00" / "Tomorrow 18:30" / "Mon 12 May 07:15". */
private fun formatDueDateTime(epochMs: Long): String = "${formatDate(epochMs)} ${formatTime(epochMs)}"

/** "Today" / "Tomorrow" / "Mon 12 May". */
private fun formatDate(epochMs: Long): String {
    val due = Calendar.getInstance().apply { timeInMillis = epochMs }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    return when {
        due.isSameDay(today) -> "Today"
        due.isSameDay(tomorrow) -> "Tomorrow"
        else -> {
            val day = due.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()).orEmpty()
            val month = due.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()).orEmpty()
            "$day ${due.get(Calendar.DAY_OF_MONTH)} $month"
        }
    }
}

private fun formatTime(epochMs: Long): String {
    val due = Calendar.getInstance().apply { timeInMillis = epochMs }
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        due.get(Calendar.HOUR_OF_DAY),
        due.get(Calendar.MINUTE),
    )
}

private fun Calendar.isSameDay(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

private const val DEFAULT_HOUR = 9
