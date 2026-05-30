package com.calmlauncher.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * Manage Apps: a per-app control surface listing every installed app. Each row shows the app
 * label with a "visible" [CalmToggle] on the trailing edge (inverted from [AppEntry.isHidden]).
 * Below the label a small tappable category chip cycles the app's [AppCategory] on click, and a
 * "Favorite" toggle pins/unpins it from the home shortcuts.
 */
@Composable
fun ManageAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageAppsViewModel = hiltViewModel(),
) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Manage Apps", onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(apps, key = { it.packageName }) { app ->
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

/**
 * A single manage-apps entry: the [SettingRow] carries the label + visibility toggle, and a
 * secondary line underneath holds the tappable category chip and the favourite toggle. Kept in
 * one composable so the per-app affordances stay grouped.
 */
@Composable
private fun ManageAppRow(
    app: AppEntry,
    onToggleVisible: (Boolean) -> Unit,
    onCycleCategory: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
) {
    val categoryInteraction = remember { MutableInteractionSource() }

    Column {
        SettingRow(
            title = app.label,
            trailing = {
                CalmToggle(checked = !app.isHidden) { visible -> onToggleVisible(visible) }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.marginMobile,
                    end = Spacing.marginMobile,
                    bottom = Spacing.stackMd,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Text(
                text = app.category.label(),
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = categoryInteraction,
                        indication = null,
                        onClick = onCycleCategory,
                    ),
            )
            Text(
                text = "Favorite",
                style = CalmType.labelMd,
                color = if (app.isFavorite) CalmWhite else CalmGray,
            )
            CalmToggle(checked = app.isFavorite) { favorite -> onToggleFavorite(favorite) }
        }
    }
}

/** "DEEP" friendly title-cased category label, e.g. SOCIAL→"Social", OTHER→"Other". */
private fun AppCategory.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
