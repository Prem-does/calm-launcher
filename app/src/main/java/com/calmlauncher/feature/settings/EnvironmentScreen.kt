package com.calmlauncher.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.LauncherSettings

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnvironmentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selected = settings.environmentMode
    var detailMode by remember { mutableStateOf<EnvironmentMode?>(null) }

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
            SectionLabel("Presets")
            EnvironmentMode.entries.forEach { mode ->
                EnvironmentRow(
                    mode = mode,
                    selected = mode == selected,
                    onClick = { viewModel.update { current -> current.applyEnvironmentSetup(mode) } },
                    onLongPress = { detailMode = mode },
                )
            }

            Spacer(Modifier.height(Spacing.stackLg))
        }

        detailMode?.let { mode ->
            EnvironmentDetailSheet(
                mode = mode,
                settings = settings,
                onDismiss = { detailMode = null },
                onEdit = onBack,
                onActivate = {
                    viewModel.update { current -> current.applyEnvironmentSetup(mode) }
                    detailMode = null
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnvironmentRow(
    mode: EnvironmentMode,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress,
                )
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = mode.icon(),
                    contentDescription = null,
                    tint = CalmGray,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = mode.displayName(), style = CalmType.bodyLg, color = CalmWhite)
                    Text(
                        text = mode.summary(),
                        style = CalmType.bodyMd,
                        color = CalmGrayDim,
                        maxLines = 1,
                    )
                }
                if (selected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = CalmGray,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(text = "Current", style = CalmType.labelMd, color = CalmGray)
                    }
                }
            }
        }
        ThinDivider()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnvironmentDetailSheet(
    mode: EnvironmentMode,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scrimInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss),
        ) {
            val panelInteraction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable(interactionSource = panelInteraction, indication = null, onClick = {})
                    .background(CalmBlack)
                    .systemBarsPadding()
                    .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackMd)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.stackMd),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = mode.icon(),
                        contentDescription = null,
                        tint = CalmGray,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(text = mode.displayName(), style = CalmType.headlineMd, color = CalmWhite)
                }

                Text(text = mode.summary(), style = CalmType.bodyMd, color = CalmGray)
                ThinDivider()

                DetailSection(title = "Allowed", lines = mode.allowedCategories())
                DetailSection(title = "Blocked", lines = mode.blockedCategories())
                mode.focusBehavior(settings.focusDurationMinutes)?.let { behavior ->
                    DetailSection(title = "Focus Behavior", lines = listOf(behavior), bulleted = false)
                }
                DetailSection(title = "Schedule", lines = listOf("Manual"), bulleted = false)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                ) {
                    CalmButton(
                        text = "Edit",
                        style = CalmButtonStyle.Outlined,
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                    )
                    CalmButton(
                        text = "Activate",
                        style = CalmButtonStyle.Filled,
                        onClick = onActivate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    lines: List<String>,
    bulleted: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
        Text(text = title, style = CalmType.labelLg, color = CalmGray)
        lines.forEach { line ->
            Text(
                text = if (bulleted) "• $line" else line,
                style = CalmType.bodyMd,
                color = CalmWhite,
            )
        }
    }
    ThinDivider()
}

private fun EnvironmentMode.summary(): String = when (this) {
    EnvironmentMode.NONE -> "Default launcher behavior."
    EnvironmentMode.WORK -> "Work essentials only."
    EnvironmentMode.STUDY -> "Study without distractions."
    EnvironmentMode.DEEP_WORK -> "Maximum focus for deep sessions."
    EnvironmentMode.SLEEP -> "Wind down."
    EnvironmentMode.GYM -> "Workout essentials."
    EnvironmentMode.OUTSIDE -> "Practical tools for errands and walks."
    EnvironmentMode.TRAVEL -> "Navigation and tickets first."
}

private fun EnvironmentMode.icon(): ImageVector = when (this) {
    EnvironmentMode.NONE -> Icons.Default.RadioButtonUnchecked
    EnvironmentMode.WORK -> Icons.Default.WorkOutline
    EnvironmentMode.STUDY -> Icons.Default.School
    EnvironmentMode.DEEP_WORK -> Icons.Default.CenterFocusStrong
    EnvironmentMode.SLEEP -> Icons.Default.Bedtime
    EnvironmentMode.GYM -> Icons.Default.FitnessCenter
    EnvironmentMode.OUTSIDE -> Icons.Default.DirectionsWalk
    EnvironmentMode.TRAVEL -> Icons.Default.FlightTakeoff
}

private fun EnvironmentMode.allowedCategories(): List<String> = when (this) {
    EnvironmentMode.NONE -> AppCategory.entries.map { it.label() }
    EnvironmentMode.WORK, EnvironmentMode.STUDY -> listOf(
        AppCategory.TOOL.label(),
        AppCategory.COMMUNICATION.label(),
        AppCategory.BROWSER.label(),
        AppCategory.STORE.label(),
        AppCategory.OTHER.label(),
    )
    EnvironmentMode.DEEP_WORK -> listOf(
        AppCategory.TOOL.label(),
        AppCategory.COMMUNICATION.label(),
        AppCategory.OTHER.label(),
    )
    EnvironmentMode.SLEEP -> listOf(
        AppCategory.TOOL.label(),
        AppCategory.COMMUNICATION.label(),
        AppCategory.OTHER.label(),
    )
    EnvironmentMode.GYM, EnvironmentMode.OUTSIDE, EnvironmentMode.TRAVEL -> listOf(
        AppCategory.TOOL.label(),
        AppCategory.COMMUNICATION.label(),
        AppCategory.BROWSER.label(),
        AppCategory.STORE.label(),
        AppCategory.OTHER.label(),
    )
}

private fun EnvironmentMode.blockedCategories(): List<String> = when (this) {
    EnvironmentMode.NONE -> listOf("None")
    EnvironmentMode.WORK, EnvironmentMode.STUDY -> listOf(
        AppCategory.SOCIAL.label(),
        AppCategory.ENTERTAINMENT.label(),
        AppCategory.GAME.label(),
    )
    EnvironmentMode.DEEP_WORK -> listOf(
        AppCategory.SOCIAL.label(),
        AppCategory.ENTERTAINMENT.label(),
        AppCategory.BROWSER.label(),
        AppCategory.STORE.label(),
        AppCategory.GAME.label(),
    )
    EnvironmentMode.SLEEP -> listOf(
        AppCategory.SOCIAL.label(),
        AppCategory.ENTERTAINMENT.label(),
        AppCategory.BROWSER.label(),
        AppCategory.GAME.label(),
    )
    EnvironmentMode.GYM, EnvironmentMode.OUTSIDE, EnvironmentMode.TRAVEL -> listOf("None by default")
}

private fun EnvironmentMode.focusBehavior(focusDurationMinutes: Int): String? = when (this) {
    EnvironmentMode.NONE -> null
    EnvironmentMode.GYM, EnvironmentMode.OUTSIDE, EnvironmentMode.TRAVEL ->
        "Focus length: ${focusDurationMinutes}m. Social and games are blocked while focus is active."
    else -> "Focus length: ${focusDurationMinutes}m."
}

private fun AppCategory.label(): String = when (this) {
    AppCategory.TOOL -> "Tools"
    AppCategory.COMMUNICATION -> "Communication"
    AppCategory.SOCIAL -> "Social"
    AppCategory.ENTERTAINMENT -> "Entertainment"
    AppCategory.BROWSER -> "Browser"
    AppCategory.STORE -> "Store"
    AppCategory.GAME -> "Games"
    AppCategory.OTHER -> "Other"
}