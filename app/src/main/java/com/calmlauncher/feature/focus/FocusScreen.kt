package com.calmlauncher.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.EInkBackdrop
import com.calmlauncher.core.designsystem.component.HoldToConfirm
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.feature.reflection.ReflectionField
import com.calmlauncher.feature.reflection.ReflectionViewModel

/** Opacity of the deliberately de-emphasised time strip at the very top of the canvas. */
private const val DimTimeAlpha = 0.2f

/**
 * Focus Mode: a pure-black, single-purpose canvas built to hold attention. On enter it
 * starts a session (if one isn't already running) so quick-exit apps stay hard-blocked.
 * A faint e-ink texture sits underneath; the day's calming quote is centered; and the only
 * way out is a deliberate hold on the bottom "End Focus" control, which fills a thin
 * progress bar before confirming. The whole surface desaturates when the restriction state
 * enforces grayscale. There is no bottom navigation here by design.
 *
 * @param onExit invoked once the user completes the hold-to-exit gesture.
 */
@Composable
fun FocusScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reflectionViewModel: ReflectionViewModel = hiltViewModel()
    val reflectionState by reflectionViewModel.uiState.collectAsStateWithLifecycle()
    var intentionDraft by remember { mutableStateOf("") }
    var editingIntention by remember { mutableStateOf(false) }
    var reflectionOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {}
    LaunchedEffect(Unit) { viewModel.startIfNeeded() }
    LaunchedEffect(state.intention) { intentionDraft = state.intention }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalmBlack)
    ) {
        // Bottom layer: barely-perceptible e-ink speckle, never intercepts input. Only drawn
        // while the E-Ink setting is on — turning it off leaves a plain black canvas.
        if (state.einkTexture) {
            EInkBackdrop(Modifier.matchParentSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FocusSessionPulse(state = state)

            FocusIntention(
                savedIntention = state.intention,
                draft = intentionDraft,
                editing = editingIntention,
                onDraftChange = { intentionDraft = it.take(120) },
                onEdit = { editingIntention = true },
                onSave = {
                    viewModel.saveIntention(intentionDraft)
                    editingIntention = false
                },
            )
            // Centered, day-stable quote — the sole focal point.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = Spacing.stackLg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Text(
                text = state.quote,
                style = CalmType.headlineMd,
                color = CalmWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile),
            )

            CalmButton(
                text = if (reflectionOpen) "HIDE REFLECTION" else "CAPTURE A THOUGHT",
                onClick = { reflectionOpen = !reflectionOpen },
                style = CalmButtonStyle.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackLg),
            )
            if (reflectionOpen && reflectionState.prompt.isNotBlank()) {
                Text(
                    text = reflectionState.prompt,
                    style = CalmType.bodyMd,
                    color = CalmWhite.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.marginMobile)
                        .padding(top = Spacing.stackLg),
                )
            }

            if (reflectionOpen) {
                ReflectionField(
                    value = reflectionState.response,
                    onValueChange = reflectionViewModel::onResponseChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.marginMobile),
                )
                CalmButton(
                    text = "SAVE THOUGHT",
                    onClick = reflectionViewModel::save,
                    style = CalmButtonStyle.Filled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.marginMobile, vertical = Spacing.gutter),
                )
                if (reflectionState.saveStatusText.isNotBlank()) {
                    Text(
                        text = reflectionState.saveStatusText,
                        style = CalmType.labelMd,
                        color = CalmWhite.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile),
                    )
                }
            }

            }

            Spacer(modifier = Modifier.height(Spacing.stackSm))

            // Deliberate hold-to-exit control with its own progress bar + sub-label.
            HoldToConfirm(
                label = "End Focus",
                subLabel = "Hold to exit",
                holdMillis = 3000L,
                onConfirm = {
                    reflectionViewModel.save()
                    viewModel.endFocus()
                    onExit()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile)
                    .padding(bottom = Spacing.stackLg),
            )
        }
    }
}

@Composable
private fun FocusIntention(
    savedIntention: String,
    draft: String,
    editing: Boolean,
    onDraftChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile)) {
        Text(text = "ONE THING", style = CalmType.labelMd, color = CalmWhite.copy(alpha = 0.6f))
        if (savedIntention.isNotBlank() && !editing) {
            Text(
                text = savedIntention,
                style = CalmType.bodyLg,
                color = CalmWhite,
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
            CalmButton(
                text = "CHANGE",
                onClick = onEdit,
                style = CalmButtonStyle.Text,
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
        } else {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = CalmType.bodyLg.copy(color = CalmWhite),
                cursorBrush = SolidColor(CalmWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.stackSm)
                    .border(1.dp, CalmGray)
                    .padding(Spacing.gutter),
                decorationBox = { inner ->
                    if (draft.isBlank()) {
                        Text("What will make this session count?", style = CalmType.bodyLg, color = CalmGray)
                    }
                    inner()
                },
            )
            CalmButton(
                text = "SET INTENTION",
                onClick = onSave,
                style = CalmButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.gutter),
            )
        }
    }
}

@Composable
private fun FocusSessionPulse(state: FocusUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("FOCUS SESSION", style = CalmType.labelMd, color = CalmWhite.copy(alpha = 0.6f))
        Text(
            text = state.remainingText,
            style = CalmType.headlineLgMobile,
            color = CalmWhite,
            modifier = Modifier.padding(top = Spacing.stackSm),
        )
        Text(
            text = "remaining  •  ${state.elapsedText} invested",
            style = CalmType.labelMd,
            color = CalmGray,
            modifier = Modifier.padding(top = Spacing.stackSm),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.gutter)
                .height(4.dp)
                .background(CalmGray),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progressFraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(CalmWhite),
            )
        }
    }
}
