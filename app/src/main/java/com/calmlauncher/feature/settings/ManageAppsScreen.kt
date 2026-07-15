package com.calmlauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry

/**
 * Manage Apps: a compact per-app control surface. Each row shows the app label and category,
 * a visibility switch, and a heart button that pins/unpins the app from home favorites.
 */
@Composable
fun ManageAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageAppsViewModel = hiltViewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.label.contains(trimmedQuery, ignoreCase = true) ||
                    app.category.label().contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Manage Apps", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ManageAppsSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.base,
                        top = Spacing.base,
                        end = Spacing.base,
                        bottom = Spacing.stackSm,
                    ),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.stackMd),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    ManageAppRow(
                        app = app,
                        onToggleVisible = { visible ->
                            viewModel.setHidden(app.packageName, !visible)
                        },
                        onCycleCategory = { viewModel.cycleCategory(app) },
                        onToggleFavorite = { favorite ->
                            viewModel.setFavorite(app.packageName, favorite)
                        },
                    )
                }
            }
        }
    }
}

/**
 * A single compact manage-apps entry. The category label remains tappable so power users can
 * continue cycling app categories without adding another control row.
 */
@Composable
private fun ManageAppRow(
    app: AppEntry,
    onToggleVisible: (Boolean) -> Unit,
    onCycleCategory: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
) {
    val categoryInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = Spacing.base),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = app.label,
                style = CalmType.labelLg,
                color = CalmWhite,
                maxLines = 1,
            )
            Text(
                text = app.category.label(),
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.clickable(
                    interactionSource = categoryInteraction,
                    indication = null,
                    onClick = onCycleCategory,
                ),
            )
        }
        CalmToggle(checked = !app.isHidden) { visible -> onToggleVisible(visible) }
        Spacer(modifier = Modifier.size(Spacing.base))
        IconButton(
            onClick = { onToggleFavorite(!app.isFavorite) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (app.isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = if (app.isFavorite) {
                    "Remove ${app.label} from favorites"
                } else {
                    "Add ${app.label} to favorites"
                },
                tint = CalmWhite,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ManageAppsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val textStyle = CalmType.labelMd.copy(color = CalmWhite)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(CalmWhite),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .background(CalmSurfaceContainer, RoundedCornerShape(6.dp))
                    .border(1.dp, CalmGrayDim, RoundedCornerShape(6.dp))
                    .padding(horizontal = Spacing.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = CalmGray,
                    modifier = Modifier.size(15.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = "Search apps...",
                            style = textStyle.copy(color = CalmGray),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

/** "DEEP" friendly title-cased category label, e.g. SOCIAL -> "Social", OTHER -> "Other". */
private fun AppCategory.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
