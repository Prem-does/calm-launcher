package com.calmlauncher.feature.reset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.component.EInkBackdrop
import com.calmlauncher.core.designsystem.component.HoldToConfirm
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.core.util.Quotes
import java.util.concurrent.TimeUnit

/**
 * The "dead end" the launch pipeline routes to instead of opening a high-friction app:
 * a gentle, unhurried black screen meant to dissolve the urge rather than feed it. A
 * faint e-ink texture sits beneath a single calming line, an optional one-line journal
 * prompt ("What were you hoping to find?"), and a deliberate hold-to-return control at
 * the bottom. Nothing here is timed or rushed — the user leaves only when they choose to.
 *
 * @param onDone invoked once the user completes the hold-to-return gesture.
 */
@Composable
fun DeadEndResetScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stable within a session: pick the reset line deterministically by day.
    val quote = remember {
        val dayIndex = (System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)).toInt()
        Quotes.resetForIndex(dayIndex)
    }
    var journal by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalmBlack),
    ) {
        // Barely-perceptible texture; never intercepts input.
        EInkBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = Spacing.marginMobile),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // The calming line, centered and given room to breathe.
            Text(
                text = quote,
                style = CalmType.headlineMd,
                color = CalmWhite,
                textAlign = TextAlign.Center,
            )

            // Optional journaling: a single-line field with only a bottom border.
            BasicTextField(
                value = journal,
                onValueChange = { journal = it },
                singleLine = true,
                textStyle = CalmType.bodyLg.copy(color = CalmWhite),
                cursorBrush = SolidColor(CalmWhite),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.stackLg)
                    .drawBehind {
                        val y = size.height
                        drawLine(
                            color = CalmGray,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(vertical = Spacing.base),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (journal.isEmpty()) {
                            Text(
                                text = "What were you hoping to find?",
                                style = CalmType.bodyLg,
                                color = CalmGray,
                            )
                        }
                        inner()
                    }
                },
            )
        }

        // Deliberate return gesture, anchored to the bottom and clear of system bars.
        HoldToConfirm(
            label = "I'm done",
            onConfirm = onDone,
            subLabel = "Hold to return",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .imePadding()
                .padding(bottom = Spacing.stackLg),
        )
    }
}
