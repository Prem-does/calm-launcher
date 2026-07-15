package com.calmlauncher.feature.applist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.AlphabetSideIndex
import com.calmlauncher.core.designsystem.component.CalmBottomNav
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmStatusBar
import com.calmlauncher.core.designsystem.component.rememberAlphabetIndex
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.navigation.Routes

/**
 * The App List: a sparse, centered, text-only drawer of launchable apps. It intentionally
 * avoids icons, badges and secondary metadata so each app name reads like one deliberate choice.
 * Taps open through the friction pipeline; long-press opens settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val alphabetIndex = rememberAlphabetIndex(state.apps) { it.label }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmStatusBar(batteryText = state.batteryText, signalText = state.signalText) },
        bottomBar = { CalmBottomNav(current = Routes.APPS, onSelect = onSelectTab) },
    ) { padding ->
        if (state.apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No visible apps. Re-enable apps in Settings > Manage Apps.",
                    style = CalmType.bodyLg,
                    color = CalmGray,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppMenuHorizontalPadding,
                        top = AppMenuVerticalCanvasPadding,
                        end = AppMenuIndexSafePadding,
                        bottom = AppMenuVerticalCanvasPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppMenuItemGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppMenuLabel(
                            app = app,
                            onClick = { viewModel.open(app) },
                            onLongClick = { onOpenSettings() },
                        )
                    }
                }

                AlphabetSideIndex(
                    index = alphabetIndex,
                    listState = listState,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private val AppMenuVerticalCanvasPadding = 128.dp
private val AppMenuHorizontalPadding = 48.dp
private val AppMenuIndexSafePadding = 72.dp
private val AppMenuItemGap = 40.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppMenuLabel(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Text(
        text = app.label,
        style = CalmType.headlineMd.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp,
            lineHeight = 40.sp,
        ),
        color = CalmWhite,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 4.dp),
    )
}
