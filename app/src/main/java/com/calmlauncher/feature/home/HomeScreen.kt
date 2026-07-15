package com.calmlauncher.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmStatusBar
import com.calmlauncher.core.designsystem.component.HomeDockNav
import com.calmlauncher.core.designsystem.component.HomeShortcutRow
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.navigation.Routes
import androidx.compose.material3.Text

/**
 * The launcher home: a pure-black canvas with the oversized clock and date centered, the
 * user's favorite shortcuts stacked toward the bottom, and an optional neutral Calm AI line.
 * Long-pressing the clock/date opens Settings; a downward swipe on the canvas opens Search.
 * The whole surface desaturates when the restriction state enforces grayscale.
 *
 * @param onSelectTab invoked with a [Routes] tab id from the bottom navigation.
 * @param onOpenSettings invoked on a long-press of the clock/date block.
 * @param onOpenSearch invoked on a downward swipe of the content.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clockInteraction = remember { MutableInteractionSource() }

    CalmScaffold(
        modifier = modifier,
        topBar = {
            CalmStatusBar(
                batteryText = state.batteryText,
                signalText = state.signalText,
            )
        },
        bottomBar = {
            HomeDockNav(
                current = Routes.HOME,
                onSelect = onSelectTab,
            )
        },
    ) { innerPadding ->
        val timeText = state.time.ifBlank { "--:--" }
        val dateText = state.date.ifBlank { "Loading home..." }
        val screenTimeText = state.screenTimeText.ifBlank { "0m today" }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 40f) onOpenSearch()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        // Rightward drag (positive) opens Settings.
                        if (dragAmount > 40f) onOpenSettings()
                    }
                }
                .padding(horizontal = Spacing.marginMobile),
        ) {
            // Push the clock toward vertical center.
            Spacer(Modifier.weight(1f))

            // Clock + date. Long-press anywhere on this block opens Settings.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = clockInteraction,
                        indication = null,
                        onClick = {},
                        onLongClick = onOpenSettings,
                    ),
            ) {
                Text(
                    text = timeText,
                    style = CalmType.heroTime,
                    color = CalmWhite,
                )
                Text(
                    text = dateText,
                    style = CalmType.headlineMd,
                    color = CalmGray,
                )
                Text(
                    text = screenTimeText,
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )
            }

            // Separate the time block from the shortcut list.
            Spacer(Modifier.weight(1f))

            // Optional neutral Calm AI insight line.
            state.insight?.let { insight ->
                Text(
                    text = insight,
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.padding(bottom = Spacing.stackMd),
                )
            }

            // Favorite shortcuts (oversized Swiss list).
            if (state.favorites.isEmpty()) {
                Text(
                    text = "No pinned apps yet. Open Settings to manage favorites.",
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.padding(bottom = Spacing.stackSm),
                )
            } else {
                state.favorites.forEach { app ->
                    HomeShortcutRow(
                        label = app.label,
                        onClick = { viewModel.open(app) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}
