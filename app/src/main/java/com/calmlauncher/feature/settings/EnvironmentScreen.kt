package com.calmlauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * Environment picker: one selectable row per [EnvironmentMode] (None / Study / Sleep / Gym /
 * Deep Work / Outside). The active mode is shown as an inverted block (white fill, black text);
 * tapping a row writes it back via [SettingsViewModel.update]. A short description under each
 * title hints at how that context re-shapes visibility and blocking.
 */
@Composable
fun EnvironmentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val restriction by viewModel.restriction.collectAsStateWithLifecycle()
    val selected = settings.environmentMode

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Environment", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            EnvironmentMode.entries.forEach { mode ->
                EnvironmentRow(
                    title = mode.displayName(),
                    description = mode.description(),
                    selected = mode == selected,
                    onClick = { viewModel.update { it.copy(environmentMode = mode) } },
                )
            }
        }
    }
}

@Composable
private fun EnvironmentRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val background = if (selected) CalmWhite else CalmBlack
    val titleColor = if (selected) CalmBlack else CalmWhite
    val bodyColor = if (selected) CalmBlack else CalmGray

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .background(background)
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        ) {
            Text(text = title, style = CalmType.bodyLg, color = titleColor)
            Text(
                text = description,
                style = CalmType.bodyMd,
                color = bodyColor,
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
        }
        ThinDivider()
    }
}

private fun EnvironmentMode.description(): String = when (this) {
    EnvironmentMode.NONE -> "No context preset. Your default visibility and friction apply."
    EnvironmentMode.STUDY -> "Surfaces tools and notes. Social and entertainment step back."
    EnvironmentMode.SLEEP -> "A dark, quiet launcher. Almost everything is tucked away."
    EnvironmentMode.GYM -> "Music and timers stay close. The rest fades out."
    EnvironmentMode.DEEP_WORK -> "Only the essentials. Distracting apps are hidden entirely."
    EnvironmentMode.OUTSIDE -> "Maps and camera up front for time spent away from the screen."
}
