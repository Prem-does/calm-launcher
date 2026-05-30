package com.calmlauncher.feature.applist

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.AppListRow
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmStatusBar
import com.calmlauncher.core.designsystem.component.CalmBottomNav
import com.calmlauncher.core.designsystem.grayscale
import com.calmlauncher.domain.model.AppDisplayMode
import com.calmlauncher.navigation.Routes

/**
 * The App List: a full drawer of every launchable app, alphabetical, text-only by default.
 * When [AppListUiState.displayMode] is [AppDisplayMode.ICONS] each row also renders the app's
 * icon via the [AppListRow] leading slot. The status bar shows battery/signal, the bottom nav
 * marks [Routes.APPS] as current, and the whole surface desaturates when the restriction state
 * demands grayscale. Taps open through the friction pipeline; long-press opens settings.
 */
@Composable
fun AppListScreen(
    onSelectTab: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier.grayscale(
            enabled = state.restriction.grayscale,
            amount = state.restriction.grayscaleAmount,
        ),
        topBar = { CalmStatusBar(batteryText = state.batteryText, signalText = state.signalText) },
        bottomBar = { CalmBottomNav(current = Routes.APPS, onSelect = onSelectTab) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            items(state.apps, key = { it.packageName }) { app ->
                AppListRow(
                    label = app.label,
                    onClick = { viewModel.open(app) },
                    onLongClick = { onOpenSettings() },
                    leading = if (state.displayMode == AppDisplayMode.ICONS) {
                        { AppIcon(viewModel.iconFor(app.packageName)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** A 24dp app icon for the ICONS display mode. Renders nothing visible when [drawable] is null. */
@Composable
private fun AppIcon(drawable: Drawable?) {
    if (drawable == null) {
        Spacer(modifier = Modifier.size(24.dp))
    } else {
        Image(
            bitmap = drawable.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}
