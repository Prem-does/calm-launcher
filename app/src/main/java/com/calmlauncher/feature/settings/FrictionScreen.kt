package com.calmlauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.FrictionLevel
import com.calmlauncher.domain.model.LauncherSettings

/** Base opening delays offered by the picker, in seconds. */
private val DelayPresets = listOf(0, 3, 5, 10, 20, 30)

/**
 * Friction: everything that stands between a tap and an app opening, on one screen. The tier
 * picker (Light / Medium / Hardcore) sits at the top as three selectable blocks — the active
 * tier is inverted, solid [CalmWhite] on [CalmBlack]. Below it are the individual delay
 * controls that used to live in Advanced Settings: the opening-delay switch and its base
 * duration, Slow Mode, and the intent prompt. Everything writes straight back through
 * [SettingsViewModel.update].
 */
@Composable
fun FrictionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selected = settings.frictionLevel

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Friction", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel("Level")
            FrictionLevel.entries.forEach { level ->
                FrictionRow(
                    title = level.title(),
                    description = level.description(),
                    selected = level == selected,
                    onClick = { viewModel.update { it.copy(frictionLevel = level) } },
                )
            }

            // --- Delays ------------------------------------------------------------------
            // These used to sit in Advanced Settings, one screen away from the tier that
            // multiplies them. They belong next to it.
            SectionLabel("Delays")
            SettingRow(
                title = "Opening Delays",
                trailing = {
                    CalmToggle(settings.openingDelaysEnabled) { checked ->
                        viewModel.update { it.copy(openingDelaysEnabled = checked) }
                    }
                },
            )
            DelayPicker(
                seconds = settings.defaultOpenDelaySeconds,
                enabled = settings.openingDelaysEnabled,
                onSelect = { seconds ->
                    viewModel.update { it.copy(defaultOpenDelaySeconds = seconds) }
                },
            )
            SettingRow(
                title = "Slow Mode",
                trailing = {
                    CalmToggle(settings.slowModeEnabled) { checked ->
                        viewModel.update { it.copy(slowModeEnabled = checked) }
                    }
                },
            )

            // --- Intent ------------------------------------------------------------------
            SectionLabel("Intent")
            SettingRow(
                title = "Intent Prompt",
                trailing = {
                    CalmToggle(settings.intentPromptEnabled) { checked ->
                        viewModel.update { it.copy(intentPromptEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Analog Mode",
                trailing = {
                    CalmToggle(settings.analogModeEnabled) { checked ->
                        viewModel.update { it.copy(analogModeEnabled = checked) }
                    }
                },
            )

            // The one line on this screen that earns its keep: it states the delay a
            // distracting app will actually get, which no single control above can show.
            EffectiveDelayLine(settings)

            Spacer(Modifier.height(Spacing.stackLg))
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
    val descriptionColor = if (selected) CalmGray else CalmGrayDim

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = CalmType.bodyLg, color = CalmWhite)
                    Text(
                        text = description,
                        style = CalmType.bodyMd,
                        color = descriptionColor,
                        modifier = Modifier.padding(top = Spacing.stackSm),
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CalmGray,
                    )
                }
            }
        }
        ThinDivider()
    }
}

/**
 * The base opening delay, as a row of tappable second values. A picker rather than a text
 * field because there is no useful delay the presets can't express, and typing a number is
 * more friction than the setting itself.
 */
@Composable
private fun DelayPicker(
    seconds: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            DelayPresets.forEach { preset ->
                DelayChoice(
                    label = if (preset == 0) "Off" else "${preset}s",
                    selected = preset == seconds,
                    enabled = enabled,
                    onClick = { onSelect(preset) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ThinDivider()
    }
}

@Composable
private fun DelayChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val alpha = if (enabled) 1f else DisabledAlpha
    val background = if (selected) CalmWhite.copy(alpha = alpha) else CalmBlack
    val textColor = if (selected) CalmBlack else CalmWhite.copy(alpha = alpha)

    Text(
        text = label,
        style = CalmType.labelMd,
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = Spacing.base),
    )
}

/** Spells out the delay a distracting app gets right now, given every control above. */
@Composable
private fun EffectiveDelayLine(settings: LauncherSettings) {
    val text = if (!settings.openingDelaysEnabled) {
        "Distracting apps open immediately."
    } else {
        // Mirrors FrictionRules.delaySeconds for a CALM risk tier: the floor the user sees
        // when nothing else is escalating. Risk can only ever add to this.
        val multiplier = when (settings.frictionLevel) {
            FrictionLevel.LIGHT -> 1
            FrictionLevel.MEDIUM -> 2
            FrictionLevel.HARDCORE -> 3
        }
        val total = settings.defaultOpenDelaySeconds.coerceAtLeast(0) * multiplier +
            if (settings.slowModeEnabled) 2 else 0
        if (total <= 0) {
            "Distracting apps open immediately."
        } else {
            "Distracting apps wait ${total}s before opening."
        }
    }

    Text(
        text = text,
        style = CalmType.labelMd,
        color = CalmGray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
    )
}

private const val DisabledAlpha = 0.4f

private fun FrictionLevel.title(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

private fun FrictionLevel.description(): String = when (this) {
    FrictionLevel.LIGHT -> "Gentle pauses. A brief delay before distracting apps open."
    FrictionLevel.MEDIUM -> "Longer delays and an intent prompt before every distracting app."
    FrictionLevel.HARDCORE -> "Maximum friction. A confirmation step and the slowest delays."
}
