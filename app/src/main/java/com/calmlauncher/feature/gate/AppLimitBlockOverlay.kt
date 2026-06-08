package com.calmlauncher.feature.gate

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppLimitStatus

@Composable
fun AppLimitBlockOverlay(
    status: AppLimitStatus,
    onGrant10Min: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var grantRequested by remember(status.packageName, status.overridesUsedToday) { mutableStateOf(false) }

    BackHandler(enabled = true) {}

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CalmBlack)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .onKeyEvent { true }
                .semantics { isTraversalGroup = true },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .focusGroup()
                    .systemBarsPadding()
                    .padding(horizontal = Spacing.marginMobile)
                    .widthIn(max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "App Limit Reached", style = CalmType.headlineLgMobile, color = CalmWhite, textAlign = TextAlign.Center)
                Text(
                    text = status.label,
                    style = CalmType.bodyLg,
                    color = CalmWhite,
                    modifier = Modifier.padding(top = Spacing.stackMd),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${status.usedMinutes}m used today of ${status.dailyLimitMinutes ?: 0}m",
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "This returns you to the home screen but won't necessarily stop background activity or notifications.",
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                    textAlign = TextAlign.Center,
                )
                if (status.canGrantOverride) {
                    CalmButton(
                        text = "Add 10 minutes and open",
                        style = CalmButtonStyle.Filled,
                        onClick = {
                            if (!grantRequested) {
                                grantRequested = true
                                onGrant10Min()
                            }
                        },
                        modifier = Modifier.padding(top = Spacing.stackLg),
                    )
                } else {
                    Text(
                        text = "You have used both 10-minute extensions for today.",
                        style = CalmType.bodyMd,
                        color = CalmGray,
                        modifier = Modifier.padding(top = Spacing.stackLg),
                        textAlign = TextAlign.Center,
                    )
                }
                CalmButton(
                    text = "Close",
                    style = CalmButtonStyle.Outlined,
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )
                // Learn more button opens usage/access settings so users can see system limits.
                val context = androidx.compose.ui.platform.LocalContext.current
                CalmButton(
                    text = "Learn more",
                    style = CalmButtonStyle.Outlined,
                    onClick = {
                        val i = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        try {
                            context.startActivity(i)
                        } catch (_: Exception) {
                            // Fall back to accessibility settings.
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )
            }
        }
    }
}
