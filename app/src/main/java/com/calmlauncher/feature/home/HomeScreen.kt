package com.calmlauncher.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.AppAction
import com.calmlauncher.core.designsystem.component.AppActionSheet
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmStatusBar
import com.calmlauncher.core.designsystem.component.HomeDockNav
import com.calmlauncher.core.designsystem.component.HomeShortcutRow
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.LocalAppearance
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.ClockStyle
import com.calmlauncher.feature.settings.displayName
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
 * @param onOpenEnvironment invoked when the active-environment chip is tapped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEnvironment: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Purely visual choices, read from the theme rather than passed in — see LocalAppearance.
    val appearance = LocalAppearance.current
    val clockStyle = appearance.clockStyle
    val gridColumns = appearance.gridColumns
    val clockInteraction = remember { MutableInteractionSource() }
    val environmentInteraction = remember { MutableInteractionSource() }
    // Long-pressing a shortcut opens the shared app menu rather than unpinning outright,
    // so it can't happen by accident.
    var actionTarget by remember { mutableStateOf<AppEntry?>(null) }

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
        BoxWithConstraints(
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Keep the clock visually centered while still allowing overflow to scroll.
                Spacer(Modifier.weight(0.65f))

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
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Clock presentation is a Customization choice. HIDDEN drops the time and
                    // date entirely but keeps this block in place, so the long-press-for-Settings
                    // gesture survives — hiding the clock must not hide the way into Settings.
                    if (clockStyle != ClockStyle.HIDDEN) {
                        Text(
                            text = timeText,
                            style = when (clockStyle) {
                                ClockStyle.COMPACT -> CalmType.headlineLgMobile
                                else -> CalmType.heroTime.copy(
                                    fontSize = 72.sp,
                                    lineHeight = 76.sp,
                                    letterSpacing = 0.sp,
                                )
                            },
                            color = CalmWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (clockStyle == ClockStyle.LARGE || clockStyle == ClockStyle.WITH_DATE) {
                        Text(
                            text = dateText,
                            style = CalmType.headlineMd,
                            color = CalmGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = screenTimeText,
                        style = CalmType.labelMd.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.sp,
                        ),
                        color = CalmGray,
                        modifier = Modifier.padding(top = Spacing.stackSm),
                        textAlign = TextAlign.Center,
                    )
                }

                // Active context preset. Hidden entirely when the environment is None, so
                // the canvas stays empty unless a mode is actually shaping behaviour.
                if (state.showEnvironment) {
                    EnvironmentChip(
                        label = state.environmentMode.displayName(),
                        interactionSource = environmentInteraction,
                        onClick = onOpenEnvironment,
                        modifier = Modifier.padding(top = Spacing.stackMd),
                    )
                }

                Spacer(Modifier.height(44.dp))

                // Optional neutral Calm AI insight line.
                state.insight?.let { insight ->
                    Text(
                        text = insight,
                        style = CalmType.labelMd,
                        color = CalmGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.stackMd),
                        textAlign = TextAlign.Center,
                    )
                }

                // Favorite shortcuts (oversized Swiss list).
                if (state.favorites.isEmpty()) {
                    Text(
                        text = "No pinned apps yet. Open Settings to manage favorites.",
                        style = CalmType.labelMd,
                        color = CalmGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.stackSm),
                    )
                } else if (gridColumns.columns <= 1) {
                    state.favorites.forEach { app ->
                        HomeShortcutRow(
                            label = app.label,
                            onClick = { viewModel.open(app) },
                            onLongClick = { actionTarget = app },
                        )
                    }
                } else {
                    // A multi-column grid, built from chunked Rows rather than a LazyVerticalGrid:
                    // the favourites list is short and already inside a height-constrained Column,
                    // and nesting a lazy grid in that would need an explicit height to measure.
                    state.favorites.chunked(gridColumns.columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                        ) {
                            row.forEach { app ->
                                HomeShortcutRow(
                                    label = app.label,
                                    onClick = { viewModel.open(app) },
                                    onLongClick = { actionTarget = app },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // Pad the final row so its items keep the column width above.
                            repeat(gridColumns.columns - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }

    actionTarget?.let { app ->
        AppActionSheet(
            appLabel = app.label,
            actions = listOf(
                AppAction(
                    label = "Remove from Home",
                    onClick = {
                        viewModel.unpin(app)
                        actionTarget = null
                    },
                ),
                AppAction(
                    label = "Open",
                    onClick = {
                        actionTarget = null
                        viewModel.open(app)
                    },
                ),
            ),
            onDismiss = { actionTarget = null },
        )
    }
}

/** A small outlined label naming the active environment preset. */
@Composable
private fun EnvironmentChip(
    label: String,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label.uppercase(),
        style = CalmType.labelMd.copy(fontSize = 11.sp, lineHeight = 14.sp),
        color = CalmWhite,
        textAlign = TextAlign.Center,
        modifier = modifier
            .border(1.dp, CalmGray, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.stackMd, vertical = 6.dp),
    )
}
