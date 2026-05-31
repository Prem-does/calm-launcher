package com.calmlauncher.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        }
    }
}
