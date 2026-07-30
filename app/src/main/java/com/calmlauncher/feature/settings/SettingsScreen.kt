package com.calmlauncher.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppDisplayMode
import com.calmlauncher.domain.model.ThemePreference

/**
 * The Settings hub: a focused (no bottom-nav) sub-page fronted by a [CalmBackBar]. A big
 * "Configuration" headline opens a single scrolling Column of [SettingRow]s — navigational
 * rows (value or chevron) at the top, then grouped toggle sections (Display / Friction /
 * Modes). Each toggle writes straight back through [SettingsViewModel.update]. The whole
 * surface desaturates when the active restriction enforces grayscale.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenManageApps: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenAppLimits: () -> Unit,
    onOpenFriction: () -> Unit,
    onOpenEnvironment: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenCustomization: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screenTimeText by viewModel.screenTimeText.collectAsStateWithLifecycle()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themePreference by themeViewModel.themePreference.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Settings", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Configuration",
                style = CalmType.headlineLgMobile,
                color = CalmWhite,
                modifier = Modifier.padding(Spacing.marginMobile),
            )

            // --- Navigational rows -------------------------------------------------------
            SettingRow("Manage Apps", onClick = onOpenManageApps, showChevron = true)
            SettingRow("Screen Time", value = screenTimeText, onClick = onOpenScreenTime)
            SettingRow("App Limits", onClick = onOpenAppLimits, showChevron = true)
            SettingRow("Reminders", onClick = onOpenReminders, showChevron = true)
            SettingRow("Reflection", onClick = onOpenReflection, showChevron = true)
            SettingRow(
                title = "Environment",
                value = settings.environmentMode.displayName(),
                onClick = onOpenEnvironment,
            )
            // Every delay/intent control now lives behind this one row, so the value it
            // shows is the whole story rather than one setting out of five.
            SettingRow(
                title = "Friction",
                value = settings.frictionLevel.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = onOpenFriction,
            )

            // --- Display -----------------------------------------------------------------
            SectionLabel("Display")
            SettingRow(
                title = "App Icons",
                trailing = {
                    CalmToggle(settings.displayMode == AppDisplayMode.ICONS) { checked ->
                        viewModel.update {
                            it.copy(
                                displayMode = if (checked) AppDisplayMode.ICONS else AppDisplayMode.TEXT,
                            )
                        }
                    }
                },
            )

            // Appearance as an expandable section with Greyscale and E-Ink options.
            val context = LocalContext.current
            val appearanceExpanded = remember { mutableStateOf(false) }
            SettingRow(
                title = "Appearance",
                value = when {
                    settings.einkSimulationEnabled -> "E-Ink"
                    settings.grayscaleEnabled -> "Greyscale"
                    else -> "Off"
                },
                onClick = { appearanceExpanded.value = !appearanceExpanded.value },
                showChevron = true,
            )

            if (appearanceExpanded.value) {
                // Greyscale is an in-app toggle so the state shown here is the state that
                // applies. Device-wide greyscale is a separate, explicit trip to Accessibility.
                SettingRow(
                    title = "Greyscale",
                    trailing = {
                        CalmToggle(settings.grayscaleEnabled) { checked ->
                            viewModel.update { it.copy(grayscaleEnabled = checked) }
                        }
                    },
                )

                SettingRow(
                    title = "System-wide Greyscale",
                    onClick = {
                        val opened = openColorCorrectionSettings(context)
                        if (!opened) {
                            Toast.makeText(context, "Unable to open color correction settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    showChevron = true,
                )

                // E-Ink remains an in-app toggle
                SettingRow(
                    title = "E-Ink",
                    trailing = {
                        CalmToggle(settings.einkSimulationEnabled) { checked ->
                            viewModel.update { it.copy(einkSimulationEnabled = checked) }
                        }
                    },
                )
            }

            // Combine Recents + Suggestions into a single, simpler toggle by default.
            SettingRow(
                title = "Show Recents & Suggestions",
                trailing = {
                    CalmToggle(settings.showRecents && settings.showSuggestions) { checked ->
                        viewModel.update { it.copy(showRecents = checked, showSuggestions = checked) }
                    }
                },
            )

            // --- Modes -------------------------------------------------------------------
            SectionLabel("Modes")
            SettingRow(
                title = "Hide Social Apps",
                trailing = {
                    CalmToggle(settings.hideSocialApps) { checked ->
                        viewModel.update { it.copy(hideSocialApps = checked) }
                    }
                },
            )
            SettingRow(
                title = "One App At A Time",
                trailing = {
                    CalmToggle(settings.oneAppAtATime) { checked ->
                        viewModel.update { it.copy(oneAppAtATime = checked) }
                    }
                },
            )

            // --- Advanced (expandable) --------------------------------------------------
            SectionLabel("Advanced")
            val advancedExpanded = remember { mutableStateOf(false) }
            SettingRow(
                title = "Advanced Settings",
                onClick = { advancedExpanded.value = !advancedExpanded.value },
                showChevron = true,
            )
            if (advancedExpanded.value) {
                AdvancedSettings(viewModel = viewModel, settings = settings)
            }

            // --- Theme -------------------------------------------------------------------
            SectionLabel("Theme")
            ThemeModeSelector(
                selected = themePreference,
                onSelected = themeViewModel::setThemePreference,
            )

            // --- Customization -----------------------------------------------------------
            // Last section on purpose. Everything above changes what the launcher *does*;
            // everything behind this row changes only how it looks, and the separation is
            // easier to trust when it is also a separation on screen.
            SectionLabel("Customization")
            SettingRow(
                title = "Appearance & Layout",
                onClick = onOpenCustomization,
                showChevron = true,
            )
            Text(
                text = "Theme, accent, fonts, grid, clock, search bar and spacing. " +
                    "Visual only — nothing here changes how the launcher behaves.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.stackMd,
                ),
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemePreference,
    onSelected: (ThemePreference) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile)
            .heightIn(min = 84.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
    ) {
        Text(
            text = "Light Mode",
            style = CalmType.bodyLg,
            fontWeight = FontWeight.Medium,
            color = if (selected == ThemePreference.LIGHT) CalmWhite else CalmGrayDim,
            modifier = Modifier.weight(1f),
        )
        ThemeModeSwitch(
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier.weight(1.15f),
        )
        Text(
            text = "Dark Mode",
            style = CalmType.bodyLg,
            fontWeight = FontWeight.Medium,
            color = if (selected == ThemePreference.DARK) CalmWhite else CalmGrayDim,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeModeSwitch(
    modifier: Modifier = Modifier,
    selected: ThemePreference,
    onSelected: (ThemePreference) -> Unit,
) {
    val darkSelected = selected == ThemePreference.DARK
    val background = if (darkSelected) Color(0xFF1F2530) else Color(0xFFF3F2EC)
    val thumbColor = if (darkSelected) Color(0xFF36404E) else Color(0xFFFAFAF4)
    val thumbTint = if (darkSelected) Color(0xFFF7F8FB) else Color(0xFF232323)
    // thumbOffset removed — BoxWithConstraints calculates thumb position using runtime width
    val trackElevation = if (darkSelected) 8.dp else 10.dp
    val interaction = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = modifier
            .shadow(
                elevation = trackElevation,
                shape = RoundedCornerShape(999.dp),
                clip = false,
                ambientColor = if (darkSelected) Color.Black.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.78f),
                spotColor = if (darkSelected) Color.Black.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.12f),
            )
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onClick = { onSelected(if (darkSelected) ThemePreference.LIGHT else ThemePreference.DARK) },
            )
            .semantics {
                contentDescription = "Theme switch"
                stateDescription = if (darkSelected) "Dark mode active" else "Light mode active"
            }
            .heightIn(min = 48.dp)
            .fillMaxWidth(),
    ) {
        val trackWidth = this.maxWidth
        val thumbSize = 40.dp
        val sidePadding = 6.dp
        val thumbX = if (darkSelected) trackWidth - thumbSize - sidePadding else sidePadding

        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .align(Alignment.CenterStart)
                .size(thumbSize)
                .shadow(
                    elevation = if (darkSelected) 6.dp else 8.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = if (darkSelected) Color.Black.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.86f),
                    spotColor = if (darkSelected) Color.Black.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.14f),
                )
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (darkSelected) Icons.Filled.DarkMode else Icons.Filled.WbSunny,
                contentDescription = null,
                tint = thumbTint,
            )
        }
    }
}

private fun openColorCorrectionSettings(context: android.content.Context): Boolean {
    val pm = context.packageManager

    val candidates = listOf(
        Intent("com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        Intent(Intent.ACTION_MAIN)
            .setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$AccessibilityDaltonizerSettingsActivity",
                ),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    for (intent in candidates) {
        if (pm.resolveActivity(intent, 0) != null) {
            context.startActivity(intent)
            return true
        }
    }

    return false
}
