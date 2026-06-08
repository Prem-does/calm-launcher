package com.calmlauncher.feature.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import java.util.Locale

private val PresetLimits = listOf(0, 15, 30, 45, 60, 120, 180)

@Composable
fun AppLimitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLimitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<AppLimitRowUiState?>(null) }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "App Limits", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(Spacing.stackSm),
        ) {
            item {
                Column(modifier = Modifier.padding(Spacing.marginMobile)) {
                    Text(text = "Daily app limits", style = CalmType.headlineLgMobile, color = CalmWhite)
                    Text(
                        text = "Limit installed apps, track foreground time, and add a short override when needed.",
                        style = CalmType.bodyMd,
                        color = CalmGray,
                        modifier = Modifier.padding(top = Spacing.stackSm),
                    )
                    SectionLabel(text = "Today")
                    Text(
                        text = "${state.summary.blockedLaunchesToday} blocked launches",
                        style = CalmType.bodyLg,
                        color = CalmWhite,
                    )
                    Text(
                        text = "${state.summary.limitedAppsToday} apps hit their limits",
                        style = CalmType.bodyMd,
                        color = CalmGray,
                    )
                    Text(
                        text = "Estimated time saved: ${state.summary.estimatedTimeSavedMinutes}m",
                        style = CalmType.bodyMd,
                        color = CalmGray,
                    )
                }
                ThinDivider()
            }

            items(state.apps, key = { it.app.packageName }) { item ->
                AppLimitRow(
                    item = item,
                    onEdit = { editor = item },
                    onToggle = { checked -> viewModel.setEnabled(item.app.packageName, checked) },
                    onRemove = { viewModel.removeLimit(item.app.packageName) },
                )
            }
        }
    }

    editor?.let { item ->
        AppLimitEditorDialog(
            item = item,
            onDismiss = { editor = null },
            onSave = { enabled, minutes ->
                viewModel.saveLimit(item.app.packageName, enabled, minutes)
                editor = null
            },
            onRemove = {
                viewModel.removeLimit(item.app.packageName)
                editor = null
            },
        )
    }
}

@Composable
private fun AppLimitRow(
    item: AppLimitRowUiState,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.marginMobile)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.gutter)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.app.label, style = CalmType.bodyLg, color = CalmWhite)
                Text(
                    text = limitStatusText(item),
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )
            }
            CalmButton(
                text = if (item.rule == null) "Set" else if (item.rule.enabled) "On" else "Off",
                style = CalmButtonStyle.Outlined,
                onClick = { onToggle(item.rule?.enabled?.not() ?: true) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.stackSm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            CalmButton(
                text = if (item.rule == null) "Add limit" else "Edit",
                style = CalmButtonStyle.Filled,
                onClick = onEdit,
            )
            if (item.rule != null) {
                CalmButton(
                    text = "Remove",
                    style = CalmButtonStyle.Outlined,
                    onClick = onRemove,
                )
            }
        }

        ThinDivider(modifier = Modifier.padding(top = Spacing.stackSm))
    }
}

private fun limitStatusText(item: AppLimitRowUiState): String = when {
    item.rule == null -> "No daily limit set"
    !item.rule.enabled -> "Limit disabled"
    item.overrideActive -> "Override active"
    item.blockedToday -> "Blocked today after ${item.usedMinutes}m"
    else -> "${item.usedMinutes}m used of ${item.limitMinutes ?: 0}m"
}

@Composable
private fun AppLimitEditorDialog(
    item: AppLimitRowUiState,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, minutes: Int) -> Unit,
    onRemove: () -> Unit,
) {
    var enabled by remember(item.app.packageName) { mutableStateOf(item.rule?.enabled ?: true) }
    var minutesText by remember(item.app.packageName) { mutableStateOf((item.limitMinutes ?: 30).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = item.app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
                Text(text = item.app.packageName, style = CalmType.labelMd, color = CalmGray)
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Daily limit (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = "Enter 0 to block this app immediately (no daily allowance).",
                    style = CalmType.bodyMd,
                    color = CalmGray,
                )
                Text(text = "Presets", style = CalmType.labelMd, color = CalmGray)
                PresetLimits.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.gutter)) {
                        row.forEach { preset ->
                            CalmButton(
                                text = formatLimitLabel(preset),
                                style = CalmButtonStyle.Outlined,
                                onClick = { minutesText = preset.toString() },
                            )
                        }
                    }
                }
                CalmButton(
                    text = if (enabled) "Limit enabled" else "Limit disabled",
                    style = CalmButtonStyle.Outlined,
                    onClick = { enabled = !enabled },
                )
            }
        },
        confirmButton = {
            CalmButton(
                text = "Save",
                style = CalmButtonStyle.Filled,
                onClick = { onSave(enabled, minutesText.toIntOrNull() ?: 30) },
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.gutter)) {
                CalmButton(text = "Cancel", style = CalmButtonStyle.Outlined, onClick = onDismiss)
                if (item.rule != null) {
                    CalmButton(text = "Remove", style = CalmButtonStyle.Outlined, onClick = onRemove)
                }
            }
        },
    )
}

private fun formatLimitLabel(minutes: Int): String = when (minutes) {
    60 -> "1 hour"
    120 -> "2 hours"
    180 -> "3 hours"
    else -> String.format(Locale.getDefault(), "%d minutes", minutes)
}
