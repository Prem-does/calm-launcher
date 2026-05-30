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
import com.calmlauncher.core.designsystem.grayscale
import com.calmlauncher.domain.model.FrictionLevel
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * Friction Level picker: three selectable tiers (Light / Monk / Hardcore). The active tier is
 * rendered as an inverted block — solid [CalmWhite] fill with [CalmBlack] text — while the rest
 * stay on the black canvas. Tapping a tier writes it back via [SettingsViewModel.update]. A
 * short description sits under each title to explain how much it tightens delays and intent.
 */
@Composable
fun FrictionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val restriction by viewModel.restriction.collectAsStateWithLifecycle()
    val selected = settings.frictionLevel

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Friction Level", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .grayscale(restriction.grayscale, restriction.grayscaleAmount)
                .verticalScroll(rememberScrollState()),
        ) {
            FrictionLevel.entries.forEach { level ->
                FrictionRow(
                    title = level.title(),
                    description = level.description(),
                    selected = level == selected,
                    onClick = { viewModel.update { it.copy(frictionLevel = level) } },
                )
            }
        }
    }
}

@Composable
private fun FrictionRow(
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

private fun FrictionLevel.title(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

private fun FrictionLevel.description(): String = when (this) {
    FrictionLevel.LIGHT -> "Gentle pauses. A brief delay before distracting apps open."
    FrictionLevel.MONK -> "Longer delays and an intent prompt before every distracting app."
    FrictionLevel.HARDCORE -> "Maximum friction. Breathing unlock and the slowest delays."
}
