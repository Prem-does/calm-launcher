package com.calmlauncher.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.AppAction
import com.calmlauncher.core.designsystem.component.AppActionSheet
import com.calmlauncher.core.designsystem.component.AppListRow
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmSearchField
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppEntry

/**
 * The Search screen: a single, autofocused query field styled with a bottom border only,
 * and a live list of matching apps beneath it. Results deliberately include hidden/social
 * apps so they stay reachable by explicit intent. Selecting a result opens it (through the
 * friction pipeline) and closes search; the [CalmBackBar] is the back affordance.
 */
@Composable
fun SearchScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    var isClosing by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<AppEntry?>(null) }
    val focusManager = LocalFocusManager.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val requestClose: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            focusManager.clearFocus()
            onClose()
        }
    }

    BackHandler(enabled = true) { requestClose() }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Search", onBack = requestClose) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -40f) requestClose()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 40f) requestClose()
                    }
                },
        ) {
            item {
                CalmSearchField(
                    query = query,
                    onQueryChange = viewModel::onQuery,
                    focusRequester = focusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Spacing.marginMobile,
                            vertical = Spacing.gutter,
                        ),
                )
            }
            items(results, key = { it.packageName }) { app ->
                AppListRow(
                    label = app.label,
                    onClick = {
                        if (!isClosing) {
                            viewModel.open(app)
                            requestClose()
                        }
                    },
                    onLongClick = { actionTarget = app },
                )
            }
        }
    }

    // Search lists every app, favourited or not, so this is the surface where a pin can
    // always be undone.
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
                    label = "Open",
                    onClick = {
                        actionTarget = null
                        if (!isClosing) {
                            viewModel.open(app)
                            requestClose()
                        }
                    },
                ),
            ),
            onDismiss = { actionTarget = null },
        )
    }
}

