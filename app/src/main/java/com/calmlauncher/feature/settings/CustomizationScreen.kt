package com.calmlauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AccentColor
import com.calmlauncher.domain.model.ClockStyle
import com.calmlauncher.domain.model.FontScale
import com.calmlauncher.domain.model.FontStyle
import com.calmlauncher.domain.model.HomeGridColumns
import com.calmlauncher.domain.model.LayoutDensity
import com.calmlauncher.domain.model.SearchBarStyle
import com.calmlauncher.domain.model.ThemeMode

/**
 * Settings → Customization: how the launcher looks, and nothing else.
 *
 * Every control here writes to [com.calmlauncher.domain.model.AppearanceSettings], which feeds the
 * theme at the composition root — so each change is visible on the next frame without restarting the
 * launcher. Nothing on this screen can alter blocking, limits, reminders, friction, gestures or
 * battery behaviour; the ViewModel has no access to the repositories that own those.
 *
 * Controls are rendered as inline choice rows rather than dropdowns or dialogs, because the value of
 * an appearance setting is the thing you want to see change. Picking a font size behind a modal
 * hides the only feedback that matters.
 */
@Composable
fun CustomizationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomizationViewModel = hiltViewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Customization", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Appearance only. These settings never change how the launcher behaves.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.stackMd,
                ),
            )

            SectionLabel("Theme")
            ChoiceRow(
                options = ThemeMode.entries,
                selected = appearance.themeMode,
                label = { it.label },
                onSelect = viewModel::setThemeMode,
            )

            SectionLabel("Accent")
            AccentRow(
                selected = appearance.accent,
                onSelect = viewModel::setAccent,
            )
            Text(
                text = "Used for selection and toggles. Text and backgrounds stay monochrome.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.base,
                ),
            )

            SectionLabel("Typography")
            SettingRow(title = "Font", value = appearance.fontStyle.label)
            ChoiceRow(
                options = FontStyle.entries,
                selected = appearance.fontStyle,
                label = { it.label },
                onSelect = viewModel::setFontStyle,
            )
            Spacer(Modifier.height(Spacing.gutter))
            SettingRow(title = "Text size", value = appearance.fontScale.label)
            ChoiceRow(
                options = FontScale.entries,
                selected = appearance.fontScale,
                label = { it.label },
                onSelect = viewModel::setFontScale,
            )

            SectionLabel("Home screen")
            SettingRow(title = "Grid layout", value = appearance.gridColumns.label)
            ChoiceRow(
                options = HomeGridColumns.entries,
                selected = appearance.gridColumns,
                label = { it.label },
                onSelect = viewModel::setGridColumns,
            )
            Spacer(Modifier.height(Spacing.gutter))
            SettingRow(title = "Clock", value = appearance.clockStyle.label)
            ChoiceRow(
                options = ClockStyle.entries,
                selected = appearance.clockStyle,
                label = { it.label },
                onSelect = viewModel::setClockStyle,
            )

            SectionLabel("Search bar")
            ChoiceRow(
                options = SearchBarStyle.entries,
                selected = appearance.searchBarStyle,
                label = { it.label },
                onSelect = viewModel::setSearchBarStyle,
            )

            SectionLabel("Spacing")
            ChoiceRow(
                options = LayoutDensity.entries,
                selected = appearance.density,
                label = { it.label },
                onSelect = viewModel::setDensity,
            )

            SectionLabel("Reset")
            SettingRow(
                title = "Restore default appearance",
                onClick = viewModel::resetToDefaults,
                showChevron = true,
            )
            Text(
                text = "Resets the options on this screen only.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.base,
                ),
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}

/**
 * A row of mutually exclusive options. The selected one inverts to solid [CalmWhite] on
 * [CalmBlack], matching the friction tier picker so the two screens read as the same launcher.
 *
 * Options wrap onto as many rows as they need rather than scrolling horizontally — a hidden option
 * in an appearance picker is one the user never discovers.
 */
@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        options.chunked(CHOICES_PER_ROW).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                row.forEach { option ->
                    ChoiceChip(
                        text = label(option),
                        selected = option == selected,
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep the last row's chips the same width as the rows above rather than letting
                // two options stretch across the screen.
                repeat(CHOICES_PER_ROW - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .background(
                color = if (selected) CalmWhite else Color.Transparent,
                shape = RoundedCornerShape(Spacing.base),
            )
            .border(
                width = 1.dp,
                color = if (selected) CalmWhite else CalmDivider,
                shape = RoundedCornerShape(Spacing.base),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = Spacing.stackMd, horizontal = Spacing.base),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CalmType.labelMd,
            color = if (selected) CalmBlack else CalmWhite,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Accent swatches. "None" is shown as an outlined circle — the absence of colour is a choice. */
@Composable
private fun AccentRow(
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile),
        horizontalArrangement = Arrangement.spacedBy(Spacing.stackMd),
    ) {
        AccentColor.entries.forEach { accent ->
            val interaction = remember { MutableInteractionSource() }
            val isSelected = accent == selected
            Box(
                modifier = Modifier
                    .size(SWATCH_OUTER)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CalmWhite else CalmDivider,
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(accent) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!accent.isMonochrome) {
                    Box(
                        modifier = Modifier
                            .size(SWATCH_INNER)
                            .background(Color(accent.darkArgb), CircleShape),
                    )
                }
            }
        }
    }
}

/** Four across fits the longest labels ("Three columns", "Time and date") at the default size. */
private const val CHOICES_PER_ROW = 4

private val SWATCH_OUTER = 40.dp
private val SWATCH_INNER = 24.dp
