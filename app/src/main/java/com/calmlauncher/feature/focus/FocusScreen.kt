package com.calmlauncher.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calmlauncher.core.designsystem.component.HoldToConfirm
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.FocusNotification
import com.calmlauncher.feature.reflection.ReflectionViewModel

private enum class QuickTarget { PUSHUPS, CHAPTERS, DEEP_WORK, TASKS, PAGES, REFLECTION }

@Composable
fun FocusScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reflectionViewModel: ReflectionViewModel = hiltViewModel()
    val reflectionState by reflectionViewModel.uiState.collectAsStateWithLifecycle()
    var intention by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<QuickTarget?>(null) }
    var trackerTarget by remember { mutableIntStateOf(50) }
    var trackerCount by remember { mutableIntStateOf(0) }
    var reflectionError by remember { mutableStateOf(false) }
    var pushups by remember { mutableIntStateOf(50) }
    var chapters by remember { mutableIntStateOf(3) }
    var tasks by remember { mutableIntStateOf(5) }
    var pages by remember { mutableIntStateOf(20) }
    var timerEditorOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {}
    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    LaunchedEffect(state.intention) { if (selected == null) intention = state.intention }

    fun label(target: QuickTarget) = when (target) {
        QuickTarget.PUSHUPS -> "$pushups pushups"
        QuickTarget.CHAPTERS -> "$chapters chapters"
        QuickTarget.DEEP_WORK -> "Deep work"
        QuickTarget.TASKS -> "$tasks tasks"
        QuickTarget.PAGES -> "Read $pages pages"
        QuickTarget.REFLECTION -> "Reflection"
    }
    fun count(target: QuickTarget) = when (target) {
        QuickTarget.PUSHUPS -> pushups
        QuickTarget.CHAPTERS -> chapters
        QuickTarget.TASKS -> tasks
        QuickTarget.PAGES -> pages
        else -> 0
    }
    fun select(target: QuickTarget) {
        if (selected == target) {
            selected = null
            intention = ""
            viewModel.saveIntention("")
            return
        }
        selected = target
        intention = label(target)
        viewModel.saveIntention(intention)
        if (target != QuickTarget.DEEP_WORK && target != QuickTarget.REFLECTION) {
            trackerTarget = count(target)
            trackerCount = 0
        }
    }
    fun cycle(target: QuickTarget) {
        val values = when (target) {
            QuickTarget.PUSHUPS -> listOf(10, 25, 50, 70, 80)
            QuickTarget.CHAPTERS -> listOf(1, 3, 5, 8, 10)
            QuickTarget.TASKS -> listOf(3, 5, 8, 12, 20)
            QuickTarget.PAGES -> listOf(5, 10, 20, 30, 50)
            else -> return
        }
        fun next(value: Int) = values[(values.indexOf(value) + 1).mod(values.size)]
        when (target) {
            QuickTarget.PUSHUPS -> pushups = next(pushups)
            QuickTarget.CHAPTERS -> chapters = next(chapters)
            QuickTarget.TASKS -> tasks = next(tasks)
            QuickTarget.PAGES -> pages = next(pages)
            else -> Unit
        }
        if (selected == target) {
            trackerTarget = count(target)
            trackerCount = 0
            intention = label(target)
            viewModel.saveIntention(intention)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(CalmBlack).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.marginMobile),
    ) {
        StatusRow(state.currentTime)
        TimerBlock(state, onOpenEditor = { timerEditorOpen = true })
        IntentionField(intention) {
            intention = it.take(120)
            selected = null
            viewModel.saveIntention(intention)
        }
        MicroLabel("QUICK.", Modifier.padding(top = Spacing.stackMd))
        QuickDial(selected, pushups, chapters, tasks, pages, ::select, ::cycle)
        when (selected) {
            QuickTarget.REFLECTION -> ReflectionPanel(
                response = reflectionState.response,
                error = reflectionError,
                onChange = { reflectionError = false; reflectionViewModel.onResponseChange(it) },
                onSave = { if (reflectionState.response.isBlank()) reflectionError = true else reflectionViewModel.save() },
            )
            QuickTarget.PUSHUPS, QuickTarget.CHAPTERS, QuickTarget.TASKS, QuickTarget.PAGES ->
                TrackerPanel(trackerTarget, trackerCount) { trackerCount = it.coerceIn(0, trackerTarget) }
            else -> Unit
        }
        NotificationsSection(
            notifications = state.notifications,
            silencedCount = state.silencedNotificationCount,
        )
        Spacer(Modifier.height(Spacing.stackLg))
        HoldToConfirm(
            label = "HOLD TO EXIT",
            subLabel = null,
            holdMillis = 2500L,
            onConfirm = { reflectionViewModel.save(); viewModel.endFocus(); onExit() },
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.stackLg),
        )
    }

    if (timerEditorOpen) {
        TimerEditorDialog(
            currentMinutes = state.durationMinutes,
            onDismiss = { timerEditorOpen = false },
            onApply = { minutes ->
                viewModel.adjustRemainingMinutes(minutes - state.durationMinutes)
                timerEditorOpen = false
            },
        )
    }
}

@Composable
private fun StatusRow(time: String) {
    Row(Modifier.fillMaxWidth().padding(top = Spacing.base), verticalAlignment = Alignment.CenterVertically) {
        Text(text = time, style = CalmType.labelMd, color = CalmWhite)
        Text(text = "FOCUS ACTIVE", style = CalmType.labelMd.copy(letterSpacing = 1.2.sp), color = CalmWhite,
            modifier = Modifier.padding(start = Spacing.gutter).border(1.dp, CalmDivider, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        Spacer(Modifier.weight(1f))
        Text(text = "ADAPTIVE INTENTION", style = CalmType.labelMd.copy(letterSpacing = 1.2.sp), color = CalmGray)
    }
}

@Composable
private fun TimerBlock(state: FocusUiState, onOpenEditor: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "focus-end-pulse").animateFloat(.35f, .95f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "focus-end-alpha")
    val remaining = (1f - state.progressFraction).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(top = Spacing.stackLg), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.remainingText,
            style = CalmType.heroTime.copy(fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
            color = CalmWhite,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenEditor,
            ),
        )
        Box(Modifier.fillMaxWidth().padding(top = Spacing.stackSm).height(1.dp).alpha(if (remaining <= .2f) pulse.value else 1f).background(CalmGray)) {
            Box(Modifier.fillMaxWidth(remaining).height(1.dp).background(CalmWhite))
        }
        Text(text = "TAP TO CHANGE TIME", style = CalmType.labelMd, color = CalmGray, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun TimerEditorDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    var selectedMinutes by remember(currentMinutes) { mutableIntStateOf(currentMinutes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(CalmBlack)
                .border(1.dp, CalmDivider, RoundedCornerShape(10.dp))
                .padding(Spacing.gutter),
        ) {
            MicroLabel("CHANGE FOCUS TIME")
            Text(text = "Minutes", style = CalmType.labelMd, color = CalmGray, modifier = Modifier.padding(top = Spacing.stackMd))
            TimerWheelPicker(
                value = selectedMinutes,
                onValueChange = { selectedMinutes = it },
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.stackMd),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "CANCEL",
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ).padding(Spacing.stackSm),
                )
                Text(
                    text = "APPLY",
                    style = CalmType.labelMd,
                    color = CalmWhite,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onApply(selectedMinutes)
                        },
                    ).padding(Spacing.stackSm),
                )
            }
        }
    }
}

@Composable
private fun TimerWheelPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var dragDistance by remember { mutableStateOf(0f) }
    val tickDistance = 28f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, CalmDivider, RoundedCornerShape(8.dp))
            .pointerInput(value) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        var nextValue = value
                        dragDistance += dragAmount
                        while (dragDistance <= -tickDistance) {
                            nextValue = (nextValue + 1).coerceAtMost(720)
                            onValueChange(nextValue)
                            dragDistance += tickDistance
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        while (dragDistance >= tickDistance) {
                            nextValue = (nextValue - 1).coerceAtLeast(1)
                            onValueChange(nextValue)
                            dragDistance -= tickDistance
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    },
                    onDragEnd = { dragDistance = 0f },
                    onDragCancel = { dragDistance = 0f },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = (value - 1).coerceAtLeast(1).toString(), style = CalmType.labelMd, color = CalmGray.copy(alpha = 0.45f))
            Text(text = value.toString(), style = CalmType.headlineMd, color = CalmWhite, modifier = Modifier.padding(vertical = Spacing.stackSm))
            Text(text = (value + 1).coerceAtMost(720).toString(), style = CalmType.labelMd, color = CalmGray.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun CircleButton(text: String, onClick: () -> Unit) {
    Text(text = text, style = CalmType.bodyLg, color = CalmWhite, textAlign = TextAlign.Center,
        modifier = Modifier.size(34.dp).border(1.dp, CalmDivider, CircleShape).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick,
        ).padding(top = 4.dp))
}

@Composable
private fun IntentionField(value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.stackLg)) {
        MicroLabel("WHAT IS YOUR FOCUS RIGHT NOW?")
        BasicTextField(value, onChange, singleLine = true, textStyle = CalmType.bodyMd.copy(color = CalmWhite), cursorBrush = SolidColor(CalmWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.stackSm)
                .drawBehind {
                    drawLine(
                        color = CalmDivider,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(bottom = 6.dp),
            decorationBox = { inner -> if (value.isBlank()) Text(text = "Type anything, or pick below.", style = CalmType.bodyMd, color = CalmGray); inner() })
    }
}

@Composable
private fun QuickDial(selected: QuickTarget?, pushups: Int, chapters: Int, tasks: Int, pages: Int, onSelect: (QuickTarget) -> Unit, onCycle: (QuickTarget) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NumericChip(QuickTarget.PUSHUPS, "$pushups pushups", pushups.toString(), selected, onSelect, onCycle)
        NumericChip(QuickTarget.CHAPTERS, "$chapters chapters", chapters.toString(), selected, onSelect, onCycle)
        SimpleChip(QuickTarget.DEEP_WORK, "Deep work", selected, onSelect)
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NumericChip(QuickTarget.TASKS, "$tasks tasks", tasks.toString(), selected, onSelect, onCycle)
        NumericChip(QuickTarget.PAGES, "Read $pages pages", pages.toString(), selected, onSelect, onCycle)
        SimpleChip(QuickTarget.REFLECTION, "Reflection", selected, onSelect)
    }
}

@Composable
private fun NumericChip(target: QuickTarget, label: String, count: String, selected: QuickTarget?, onSelect: (QuickTarget) -> Unit, onCycle: (QuickTarget) -> Unit) {
    val active = selected == target
    Row(Modifier.clip(RoundedCornerShape(50)).border(1.dp, if (active) CalmWhite else CalmDivider, RoundedCornerShape(50)).background(if (active) CalmWhite else CalmBlack), verticalAlignment = Alignment.CenterVertically) {
        Text(text = count, style = CalmType.labelMd, color = if (active) CalmBlack else CalmWhite, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onCycle(target) }).padding(start = 8.dp, top = 6.dp, bottom = 6.dp))
        Text(text = label.removePrefix(count), style = CalmType.labelMd, color = if (active) CalmBlack else CalmWhite, modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onSelect(target) }).padding(end = 8.dp, top = 6.dp, bottom = 6.dp))
    }
}

@Composable
private fun SimpleChip(target: QuickTarget, label: String, selected: QuickTarget?, onSelect: (QuickTarget) -> Unit) {
    val active = selected == target
    Text(text = label, style = CalmType.labelMd, color = if (active) CalmBlack else CalmWhite, modifier = Modifier.clip(RoundedCornerShape(50)).background(if (active) CalmWhite else CalmBlack).border(1.dp, if (active) CalmWhite else CalmDivider, RoundedCornerShape(50)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { onSelect(target) }).padding(horizontal = 8.dp, vertical = 6.dp))
}

@Composable
private fun TrackerPanel(target: Int, count: Int, onChange: (Int) -> Unit) {
    Panel {
        PanelHeader("REPETITION TRACKER", "TARGET: $target")
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.stackMd), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircleButton("-", { onChange(count - 1) })
            Text(text = "$count / $target", style = CalmType.headlineMd, color = CalmWhite, modifier = Modifier.width(110.dp), textAlign = TextAlign.Center)
            CircleButton("+", { onChange(count + 1) })
        }
        Text(text = "✓  Completed", style = CalmType.labelMd, color = CalmWhite.copy(alpha = if (count >= target) 1f else .35f), modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ReflectionPanel(response: String, error: Boolean, onChange: (String) -> Unit, onSave: () -> Unit) {
    Panel {
        PanelHeader("REFLECTION", null)
        BasicTextField(
            value = response,
            onValueChange = onChange,
            textStyle = CalmType.bodyMd.copy(color = CalmWhite),
            cursorBrush = SolidColor(CalmWhite),
            modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = Spacing.stackSm),
            decorationBox = { inner ->
                if (response.isBlank()) Text(text = "What is on your mind?", style = CalmType.bodyMd, color = CalmGray)
                inner()
            },
        )
        if (error) Text(text = "Write something before saving.", style = CalmType.labelMd, color = CalmWhite, modifier = Modifier.padding(top = 4.dp))
        Text(text = "Save", style = CalmType.labelMd, color = CalmWhite, modifier = Modifier.align(Alignment.End).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSave).padding(top = Spacing.stackSm))
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.stackMd).border(1.dp, CalmDivider, RoundedCornerShape(10.dp)).padding(Spacing.gutter), content = content)
}

@Composable
private fun PanelHeader(title: String, trailing: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { MicroLabel(title); trailing?.let { Text(text = it, style = CalmType.labelMd, color = CalmGray) } }
}

@Composable
private fun NotificationsSection(
    notifications: List<FocusNotification>,
    silencedCount: Int,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Spacing.stackLg)
            .border(1.dp, CalmDivider, RoundedCornerShape(10.dp))
            .padding(Spacing.gutter),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            MicroLabel("IMPORTANT NOTIFICATIONS", Modifier.weight(1f))
            Text(
                text = "FROM PINNED APPS",
                style = CalmType.labelMd,
                color = CalmGray,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.85f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp, max = 180.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (notifications.isEmpty()) {
                Text(text = "No pinned app notifications right now.", style = CalmType.bodyMd, color = CalmWhite, modifier = Modifier.padding(top = Spacing.gutter))
            } else {
                Column {
                    notifications.forEach { notification ->
                        NotificationRow(notification)
                    }
                }
            }
        }
        Text(text = "$silencedCount other notifications silenced.", style = CalmType.labelMd, color = CalmGray, modifier = Modifier.padding(top = Spacing.stackSm))
    }
}

@Composable
private fun NotificationRow(notification: FocusNotification) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.gutter)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = notification.appName, style = CalmType.labelMd, color = CalmWhite)
            Text(text = relativeTime(notification.postedAtEpochMs), style = CalmType.labelMd, color = CalmGray)
        }
        Text(text = notification.title, style = CalmType.bodyMd, color = CalmWhite, modifier = Modifier.padding(top = 2.dp))
        if (notification.preview.isNotBlank()) {
            Text(
                text = notification.preview,
                style = CalmType.labelMd,
                color = CalmWhite.copy(alpha = 0.82f),
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun relativeTime(postedAtEpochMs: Long): String {
    val elapsedMinutes = ((System.currentTimeMillis() - postedAtEpochMs).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        elapsedMinutes <= 0 -> "now"
        elapsedMinutes == 1 -> "1m"
        elapsedMinutes < 60 -> "${elapsedMinutes}m"
        else -> "${elapsedMinutes / 60}h"
    }
}

@Composable
private fun MicroLabel(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = CalmType.labelMd.copy(letterSpacing = 1.1.sp), color = CalmGray, modifier = modifier)
}