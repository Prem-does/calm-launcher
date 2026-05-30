package com.calmlauncher.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * Screen Time: today's foreground total rendered large, then a per-app breakdown (busiest first)
 * and the Calm AI insights as neutral, non-judgemental text lines. Read-only and presentational —
 * all derivation lives in [ScreenTimeViewModel].
 */
@Composable
fun ScreenTimeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScreenTimeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Screen Time", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Today's total, oversized.
            Column(modifier = Modifier.padding(Spacing.marginMobile)) {
                Text(text = state.total, style = CalmType.headlineLgMobile, color = CalmWhite)
                Text(text = "today", style = CalmType.bodyMd, color = CalmGray)
            }

            // Per-app breakdown.
            if (state.topApps.isNotEmpty()) {
                SectionLabel("Apps")
                state.topApps.forEach { usage ->
                    UsageRow(label = usage.label, duration = usage.duration)
                }
            }

            // Calm AI insights as plain lines.
            if (state.insights.isNotEmpty()) {
                SectionLabel("Insights")
                state.insights.forEach { insight ->
                    Text(
                        text = insight.text,
                        style = CalmType.bodyLg,
                        color = CalmGray,
                        modifier = Modifier.padding(
                            horizontal = Spacing.marginMobile,
                            vertical = Spacing.rowVertical,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageRow(label: String, duration: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Text(
                text = label,
                style = CalmType.bodyLg,
                color = CalmWhite,
                modifier = Modifier.weight(1f),
            )
            Text(text = duration, style = CalmType.bodyMd, color = CalmGray)
        }
        ThinDivider()
    }
}
