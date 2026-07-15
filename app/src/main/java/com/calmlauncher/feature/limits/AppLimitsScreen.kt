package com.calmlauncher.feature.limits

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppCategory
import java.util.Locale

private val PresetLimits = listOf(0, 15, 30, 45, 60, 120, 180)

private data class AppLimitGroup(
    val title: String,
    val categories: Set<AppCategory>,
    val fallbackLimitMinutes: Int?,
)

private val LimitGroups = listOf(
    AppLimitGroup("Social Group", setOf(AppCategory.SOCIAL), 30),
    AppLimitGroup("Entertainment Group", setOf(AppCategory.ENTERTAINMENT, AppCategory.GAME), 60),
    AppLimitGroup("Information Group", setOf(AppCategory.COMMUNICATION, AppCategory.TOOL, AppCategory.OTHER), null),
    AppLimitGroup("Web Browser", setOf(AppCategory.BROWSER), null),
)

@Composable
fun AppLimitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLimitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<AppLimitRowUiState?>(null) }

    val groupRows = remember(state.apps) {
        LimitGroups.map { group ->
            group to state.apps.filter { it.app.category in group.categories }
        }
    }
    val filteredApps = remember(state.apps, query) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            emptyList()
        } else {
            state.apps.filter { item ->
                item.app.label.contains(trimmed, ignoreCase = true) ||
                    item.app.category.label().contains(trimmed, ignoreCase = true)
            }
        }
    }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "App Limits", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AppLimitMetrics(
                totalMinutesUsed = state.apps.sumOf { it.usedMinutes },
                groupsLimited = groupRows.count { (_, apps) -> apps.any { it.rule?.enabled == true } },
            )
            ThinDivider()
            AppLimitSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.stackMd),
            )
            ThinDivider()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.stackMd),
            ) {
                if (query.isBlank()) {
                    items(groupRows, key = { it.first.title }) { (group, apps) ->
                        AppLimitGroupRow(
                            group = group,
                            apps = apps,
                            limitedCount = apps.count { it.rule != null },
                            onAddApps = {
                                editor = apps.firstOrNull { it.rule == null } ?: apps.firstOrNull()
                            },
                            onEditLimit = {
                                editor = apps.firstOrNull { it.rule != null } ?: apps.firstOrNull()
                            },
                        )
                    }
                    item {
                        AddGroupRow(
                            onClick = {
                                editor = state.apps.firstOrNull { it.rule == null } ?: state.apps.firstOrNull()
                            },
                            modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.stackMd),
                        )
                    }
                } else {
                    items(filteredApps, key = { it.app.packageName }) { item ->
                        AppLimitSearchResultRow(
                            item = item,
                            onEdit = { editor = item },
                            onToggle = { checked -> viewModel.setEnabled(item.app.packageName, checked) },
                            onRemove = { viewModel.removeLimit(item.app.packageName) },
                        )
                    }
                }

                if (query.isNotBlank() && filteredApps.isEmpty()) {
                    item {
                        Text(
                            text = "No apps found",
                            style = CalmType.labelMd,
                            color = CalmGray,
                            modifier = Modifier.padding(Spacing.marginMobile),
                        )
                    }
                }
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
private fun AppLimitMetrics(
    totalMinutesUsed: Int,
    groupsLimited: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.base, vertical = Spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricBlock(value = formatTotalTime(totalMinutesUsed), label = "TOTAL TIME USED")
        MetricBlock(value = groupsLimited.toString(), label = "GROUPS LIMITED")
    }
}

@Composable
private fun MetricBlock(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = CalmType.headlineMd.copy(
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CalmWhite,
        )
        Text(
            text = label,
            style = CalmType.labelMd.copy(
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CalmGray,
        )
    }
}

@Composable
private fun AppLimitSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val textStyle = CalmType.labelMd.copy(color = CalmWhite, fontSize = 11.sp, lineHeight = 14.sp)
    val shape = RoundedCornerShape(0.dp)

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
                    .height(36.dp)
                    .background(CalmSurfaceContainer.copy(alpha = 0.45f), shape)
                    .border(1.dp, CalmGrayDim.copy(alpha = 0.55f), shape)
                    .padding(horizontal = Spacing.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = CalmGray,
                    modifier = Modifier.size(14.dp),
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

@Composable
private fun AppLimitGroupRow(
    group: AppLimitGroup,
    apps: List<AppLimitRowUiState>,
    limitedCount: Int,
    onAddApps: () -> Unit,
    onEditLimit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.base, vertical = Spacing.stackMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = CalmType.labelMd.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = CalmWhite,
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                ) {
                    Text(
                        text = formatGroupLimit(apps, group),
                        style = CalmType.headlineMd.copy(
                            fontSize = 20.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = CalmWhite,
                    )
                    TextLimitChip(
                        text = if (limitedCount == 0) "SET LIMIT" else "EDIT LIMIT",
                        onClick = onEditLimit,
                    )
                }
            }
            TextLimitChip(text = "ADD APPS", onClick = onAddApps)
        }
        ThinDivider(modifier = Modifier.padding(top = Spacing.stackMd))
    }
}

@Composable
private fun TextLimitChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .widthIn(min = 68.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(0.dp))
            .border(1.dp, CalmWhite.copy(alpha = 0.8f), RoundedCornerShape(0.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.base),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CalmType.labelMd.copy(
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CalmWhite,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddGroupRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = CalmWhite,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "Add new group of apps",
            style = CalmType.labelMd.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CalmWhite,
        )
    }
}

@Composable
private fun AppLimitSearchResultRow(
    item: AppLimitRowUiState,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.base, vertical = Spacing.stackMd),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.app.label, style = CalmType.bodyLg, color = CalmWhite, maxLines = 1)
                Text(
                    text = limitStatusText(item),
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TextLimitChip(
                text = if (item.rule == null) "ADD LIMIT" else if (item.rule.enabled) "ON" else "OFF",
                onClick = { onToggle(item.rule?.enabled?.not() ?: true) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.stackSm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            TextLimitChip(text = if (item.rule == null) "SET LIMIT" else "EDIT LIMIT", onClick = onEdit)
            if (item.rule != null) {
                TextLimitChip(text = "REMOVE", onClick = onRemove)
            }
        }
        ThinDivider(modifier = Modifier.padding(top = Spacing.stackMd))
    }
}

private fun formatGroupLimit(
    apps: List<AppLimitRowUiState>,
    group: AppLimitGroup,
): String {
    val limit = apps.firstOrNull { it.rule?.enabled == true }?.limitMinutes ?: group.fallbackLimitMinutes
    return limit?.let { formatCompactLimit(it) } ?: "No limit set"
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

private fun formatTotalTime(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}H ${minutes}M"
        hours > 0 -> "${hours}H"
        else -> "${minutes}M"
    }
}

private fun formatCompactLimit(minutes: Int): String = when {
    minutes <= 0 -> "0m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    minutes > 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}

private fun formatLimitLabel(minutes: Int): String = when (minutes) {
    60 -> "1 hour"
    120 -> "2 hours"
    180 -> "3 hours"
    else -> String.format(Locale.getDefault(), "%d minutes", minutes)
}

private fun AppCategory.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
