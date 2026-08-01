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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.AlphabetSideIndex
import com.calmlauncher.core.designsystem.component.AppAction
import com.calmlauncher.core.designsystem.component.AppActionSheet
import com.calmlauncher.core.designsystem.component.CalmBottomNav
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmStatusBar
import com.calmlauncher.core.designsystem.component.rememberAlphabetIndex
import com.calmlauncher.core.designsystem.theme.CalmAppNameTextStyle
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.feature.settings.displayName
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.navigation.Routes

/**
 * The App List: a sparse, centered, text-only drawer of launchable apps. It intentionally
 * avoids icons, badges and secondary metadata so each app name reads like one deliberate choice.
 * Taps open through the friction pipeline; long-press opens the shared app menu, which is
 * where an app is pinned to or unpinned from Home.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEnvironment: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val alphabetIndex = rememberAlphabetIndex(state.apps) { it.label }
    // Long-press opens the shared app menu rather than jumping straight to Settings, so
    // pinning is reachable from the drawer without a detour through Manage Apps.
    var actionTarget by remember { mutableStateOf<AppEntry?>(null) }

    CalmScaffold(
        modifier = modifier,
        topBar = {
            CalmStatusBar(
                batteryText = state.batteryText,
                signalText = state.signalText,
                environmentText = state.environmentMode.takeIf { it != EnvironmentMode.NONE }?.displayName(),
                onEnvironmentClick = onOpenEnvironment,
            )
        },
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
                        end = AppMenuHorizontalPadding,
                        bottom = AppMenuBottomCanvasPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppMenuItemGap),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppMenuLabel(
                            app = app,
                            onClick = { viewModel.open(app) },
                            onLongClick = { actionTarget = app },
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

    actionTarget?.let { app ->
        AppActionSheet(
            appLabel = app.label,
            actions = listOf(
                AppAction(
                    label = if (app.isFavorite) "Remove from Home" else "Add to Home",
                    onClick = {
                        viewModel.setFavorite(app, !app.isFavorite)
                        actionTarget = null
                    },
                ),
                AppAction(
                    label = "Settings",
                    onClick = {
                        actionTarget = null
                        onOpenSettings()
                    },
                ),
            ),
            onDismiss = { actionTarget = null },
        )
    }
}

private val AppMenuVerticalCanvasPadding = 128.dp
private val AppMenuBottomCanvasPadding = 184.dp
private val AppMenuHorizontalPadding = 48.dp
private val AppMenuItemGap = 36.dp

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppMenuLabel(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Text(
        text = app.label,
        style = CalmAppNameTextStyle,
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
