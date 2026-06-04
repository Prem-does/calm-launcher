package com.calmlauncher.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalmBlack),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
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
            CalmButton(
                text = "Add 10 minutes and open",
                style = CalmButtonStyle.Filled,
                onClick = onGrant10Min,
                modifier = Modifier.padding(top = Spacing.stackLg),
            )
            CalmButton(
                text = "Close",
                style = CalmButtonStyle.Outlined,
                onClick = onDismiss,
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
            // Learn more button opens usage/access settings so users can see system limits.
            val context = LocalContext.current
            CalmButton(
                text = "Learn more",
                style = CalmButtonStyle.Outlined,
                onClick = {
                    val i = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    try {
                        context.startActivity(i)
                    } catch (_: Exception) {
                        // fall back to accessibility settings
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
        }
    }
}
