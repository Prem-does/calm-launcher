package com.calmlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.calmlauncher.launcher.shouldQuietInteractions
import com.calmlauncher.ui.theme.LocalLauncherSettings
import com.calmlauncher.ui.theme.launcherAnimationEnabled
import com.calmlauncher.ui.theme.launcherMotionDurationMillis

@Composable
fun LauncherChrome(
    statusText: String,
    rightActions: List<String>,
    bottomActions: List<BottomAction>? = null,
    content: @Composable () -> Unit
) {
    val settings = LocalLauncherSettings.current
    val animated = launcherAnimationEnabled(settings)
    val motionDuration = launcherMotionDurationMillis(settings)
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (animated) 1f else 0.98f,
        animationSpec = tween(durationMillis = motionDuration.coerceAtLeast(1)),
        label = "launcherChromeBgAlpha"
    )
    Column(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha),
                    MaterialTheme.colorScheme.background.copy(alpha = 0.94f * backgroundAlpha)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(statusText, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rightActions.forEach { label ->
                    Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        ThinDivider(modifier = Modifier.fillMaxWidth())

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }

        if (bottomActions != null) {
            ThinDivider(modifier = Modifier.fillMaxWidth())
            BottomRail(bottomActions)
        }
    }
}

data class BottomAction(
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun BottomRail(actions: List<BottomAction>) {
    val settings = LocalLauncherSettings.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            val background = if (action.selected) MaterialTheme.colorScheme.primary else Color.Transparent
            val foreground = if (action.selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
            val clickHandler = remember(settings.shouldQuietInteractions(), action.onClick) {
                {
                    if (!settings.shouldQuietInteractions()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    action.onClick()
                }
            }
            Box(
                modifier = Modifier
                    .background(background)
                    .clickable(onClick = clickHandler)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    action.label,
                    color = foreground,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
