package com.calmlauncher.feature.reflection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/** Thickness of the response field's underline — matches the launcher's other bare fields. */
private val UnderlineThickness = 1.dp

/** Minimum height for the multi-line response field, so it reads as an invitation to write. */
private val ResponseFieldMinHeight = 96.dp

/**
 * The **Usage Reflection** screen — the launcher's quiet, nightly check-in (and the visible face
 * of the **Calm AI Assistant**). Tonight's gentle prompt sits above a bare, multi-line field for a
 * few unhurried words; saving persists the entry for the day. Below, the week's neutral insights
 * and today's screen time are surfaced as plain observations — never a scoreboard, never a nag.
 *
 * Pure black canvas, white-on-black type, a single hairline underline on the field; the
 * [CalmBackBar] is the only way back.
 *
 * @param onBack invoked when the user leaves the reflection.
 */
@Composable
fun ReflectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReflectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Reflection", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.marginMobile),
        ) {
            // Calm, nightly headline.
            Text(
                text = "Tonight",
                style = CalmType.headlineLgMobile,
                color = CalmWhite,
            )

            // The gentle, non-judgemental prompt for today.
            if (state.prompt.isNotBlank()) {
                Text(
                    text = state.prompt,
                    style = CalmType.bodyLg,
                    color = CalmWhite,
                    modifier = Modifier.padding(top = Spacing.gutter),
                )
            }

            // A few unhurried words — bare multi-line field, bottom border only.
            ReflectionField(
                value = state.response,
                onValueChange = viewModel::onResponseChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.gutter),
            )

            CalmButton(
                text = "Save",
                onClick = { viewModel.save() },
                style = CalmButtonStyle.Filled,
                modifier = Modifier.padding(top = Spacing.gutter),
            )
            if (state.saveStatusText.isNotBlank()) {
                Text(
                    text = state.saveStatusText,
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )
            }

            // The Calm AI Assistant's weekly observations.
            SectionLabel(text = "This week")
            state.insights.forEach { insight ->
                Text(
                    text = insight,
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    modifier = Modifier.padding(
                        horizontal = Spacing.marginMobile,
                        vertical = Spacing.stackSm,
                    ),
                )
            }

            // Today's plain screen-time observation.
            SectionLabel(text = "Today")
            Text(
                text = state.screenTimeText,
                style = CalmType.bodyMd,
                color = CalmGray,
                modifier = Modifier.padding(horizontal = Spacing.marginMobile),
            )

            SectionLabel(text = "Notes")
            if (state.recentNotes.isEmpty()) {
                Text(
                    text = "Saved reflections will appear here.",
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    modifier = Modifier.padding(horizontal = Spacing.marginMobile),
                )
            } else {
                state.recentNotes.forEach { note ->
                    ReflectionNote(note = note)
                }
            }
        }
    }
}

@Composable
private fun ReflectionNote(note: ReflectionNoteUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackSm),
    ) {
        Text(
            text = note.dayLabel,
            style = CalmType.labelMd,
            color = CalmGray,
        )
        Text(
            text = note.response,
            style = CalmType.bodyMd,
            color = CalmWhite,
            modifier = Modifier.padding(top = Spacing.stackSm),
        )
        if (note.prompt.isNotBlank()) {
            Text(
                text = note.prompt,
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
        }
    }
}

/**
 * A bare, multi-line reflection field: white text, a 1dp [CalmWhite] underline and a [CalmGray]
 * "A few words…" placeholder. No Material container — just the hairline rule, matching the
 * monochrome Search field treatment.
 */
@Composable
fun ReflectionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle: TextStyle = CalmType.bodyLg.copy(color = CalmWhite)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = ResponseFieldMinHeight)
            .drawBehind {
                val y = size.height - UnderlineThickness.toPx() / 2f
                drawLine(
                    color = CalmWhite,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = UnderlineThickness.toPx(),
                )
            }
            .padding(bottom = Spacing.base),
        textStyle = textStyle,
        cursorBrush = SolidColor(CalmWhite),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(text = "A few words…", style = textStyle.copy(color = CalmGray))
                }
                innerTextField()
            }
        },
    )
}
