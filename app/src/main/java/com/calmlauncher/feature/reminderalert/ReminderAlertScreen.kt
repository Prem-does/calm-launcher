package com.calmlauncher.feature.reminderalert

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.core.util.formatReminderDueTime
import com.calmlauncher.domain.model.Reminder

/**
 * The reminder interruption itself: the reminder, stated plainly, with the only two ways out.
 *
 * Deliberately not a dialog. A reminder the user asked for should cost them a decision, not a
 * swipe — so there is no scrim to tap through, no back affordance, and no close button. The two
 * actions are the entire exit surface, which is what separates this from the notification it
 * replaced.
 */
@Composable
fun ReminderAlertScreen(
    reminder: Reminder,
    snoozePickerOpen: Boolean,
    busy: Boolean,
    onSnoozeClick: () -> Unit,
    onSnoozeChosen: (Int) -> Unit,
    onSnoozeDismiss: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.marginMobile),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "REMINDER",
                style = CalmType.labelMd,
                color = CalmGray,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.gutter))

            Text(
                text = reminder.title,
                style = CalmType.headlineLgMobile,
                color = CalmWhite,
                textAlign = TextAlign.Center,
            )

            reminder.dueAtEpochMs?.let { due ->
                Spacer(Modifier.height(Spacing.stackMd))
                Text(
                    text = formatReminderDueTime(due),
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    textAlign = TextAlign.Center,
                )
            }

            if (reminder.note.isNotBlank()) {
                Spacer(Modifier.height(Spacing.gutter))
                Text(
                    text = reminder.note,
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(Spacing.stackLg))

            // The snooze durations replace the actions in place rather than opening a second
            // layer, so choosing one is a single tap from the decision to snooze.
            if (snoozePickerOpen) {
                SnoozeChoiceList(
                    enabled = !busy,
                    onChosen = onSnoozeChosen,
                    onCancel = onSnoozeDismiss,
                )
            } else {
                AlertAction(
                    label = "Snooze",
                    enabled = !busy,
                    onClick = onSnoozeClick,
                )
                Spacer(Modifier.height(Spacing.stackMd))
                AlertAction(
                    label = "Finished",
                    enabled = !busy,
                    onClick = onFinished,
                )
            }
        }
    }
}

@Composable
private fun SnoozeChoiceList(
    enabled: Boolean,
    onChosen: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Remind me again in",
            style = CalmType.labelMd,
            color = CalmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.stackMd))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            SnoozeChoices.forEach { minutes ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CalmGray, RoundedCornerShape(percent = 50))
                        .alpha(if (enabled) 1f else 0.4f)
                        .clickable(enabled = enabled) { onChosen(minutes) }
                        .padding(vertical = Spacing.stackMd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${minutes}m",
                        style = CalmType.bodyMd,
                        color = CalmWhite,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.gutter))
        Text(
            text = "Back",
            style = CalmType.labelMd,
            color = CalmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(enabled = enabled) { onCancel() }
                .padding(vertical = Spacing.stackMd),
        )
    }
}

@Composable
private fun AlertAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CalmWhite, RoundedCornerShape(percent = 50))
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = Spacing.gutter),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = CalmType.bodyLg, color = CalmWhite)
    }
}
