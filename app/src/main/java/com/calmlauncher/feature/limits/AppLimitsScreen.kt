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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import com.calmlauncher.core.designsystem.theme.CalmBlack
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
    val id: String,
    val title: String,
    val categories: Set<AppCategory>,
    val fallbackLimitMinutes: Int?,
)

private val LimitGroups = listOf(
    AppLimitGroup("social", "Social Group", setOf(AppCategory.SOCIAL), 30),
    AppLimitGroup("entertainment", "Entertainment Group", setOf(AppCategory.ENTERTAINMENT, AppCategory.GAME), 60),
    AppLimitGroup("information", "Information Group", setOf(AppCategory.COMMUNICATION, AppCategory.TOOL, AppCategory.OTHER), null),
    AppLimitGroup("browser", "Web Browser", setOf(AppCategory.BROWSER), null),
)

private data class AppLimitGroupEditorState(
    val group: AppLimitGroup,
    val selectedApps: List<AppLimitRowUiState>,
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
    var groupEditor by remember { mutableStateOf<AppLimitGroupEditorState?>(null) }

    val groupRows = remember(state.apps, state.groupAssignments) {
        LimitGroups.map { group ->
            val assignedToGroup = state.apps.filter { state.groupAssignments[it.app.packageName] == group.id }
            val apps = if (assignedToGroup.isNotEmpty()) {
                assignedToGroup
            } else {
                state.apps.filter { item ->
                    item.app.category in group.categories &&
                        state.groupAssignments[item.app.packageName] == null
                }
            }
            group to apps
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
                .background(CalmBlack)
                .padding(padding),
        ) {
            AppLimitHeaderCard(
                totalMinutesUsed = state.apps.sumOf { it.usedMinutes },
                groupsLimited = groupRows.count { (_, apps) -> apps.any { it.rule?.enabled == true } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.stackSm),
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
                contentPadding = PaddingValues(bottom = Spacing.stackMd, top = Spacing.stackSm),
            ) {
                if (query.isBlank()) {
                    items(groupRows, key = { it.first.title }) { (group, apps) ->
                        AppLimitGroupRow(
                            group = group,
                            apps = apps,
                            limitedCount = apps.count { it.rule != null },
                            onAddApps = {
                                groupEditor = AppLimitGroupEditorState(group, apps)
                            },
                            onEditLimit = {
                                groupEditor = AppLimitGroupEditorState(group, apps)
                            },
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

    groupEditor?.let { groupState ->
        AppLimitGroupEditorDialog(
            group = groupState.group,
            allApps = state.apps,
            selectedApps = groupState.selectedApps,
            onDismiss = { groupEditor = null },
            onSave = { packageNames, enabled, minutes ->
                viewModel.saveGroupLimit(groupState.group.id, packageNames, enabled, minutes)
                groupEditor = null
            },
        )
    }
}

@Composable
private fun AppLimitHeaderCard(
    totalMinutesUsed: Int,
    groupsLimited: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .then(modifier)
            .background(CalmSurfaceContainer.copy(alpha = 0.24f), RoundedCornerShape(10.dp))
            .border(1.dp, CalmGrayDim.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
            .padding(Spacing.base),
        verticalArrangement = Arrangement.spacedBy(Spacing.stackSm),
    ) {
        Text(
            text = "Build groups, then set one shared timer.",
            style = CalmType.bodyLg.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
            color = CalmWhite,
        )
        Text(
            text = "Add apps like YouTube, X, and Instagram to a group such as Social, then control them with one limit.",
            style = CalmType.labelMd.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = CalmGray,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
            MetricBlock(
                value = formatTotalTime(totalMinutesUsed),
                label = "TOTAL TIME USED",
                modifier = Modifier.weight(1f),
            )
            MetricBlock(
                value = groupsLimited.toString(),
                label = "GROUPS LIMITED",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .then(modifier)
            .heightIn(min = 72.dp)
            .background(CalmSurfaceContainer.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .border(1.dp, CalmGrayDim.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.base, vertical = Spacing.stackSm),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
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
private fun AppLimitIntroCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CalmSurfaceContainer.copy(alpha = 0.26f), RoundedCornerShape(8.dp))
            .border(1.dp, CalmGrayDim.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.base, vertical = Spacing.stackSm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Build a shared limit group",
            style = CalmType.bodyLg.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            ),
            color = CalmWhite,
        )
        Text(
            text = "Pick apps like YouTube, X, and Instagram, then give the whole group one daily timer.",
            style = CalmType.labelMd.copy(fontSize = 11.sp, lineHeight = 14.sp),
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
            .padding(horizontal = Spacing.base, vertical = Spacing.stackSm),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.stackSm),
            modifier = Modifier
                .fillMaxWidth()
                .background(CalmSurfaceContainer.copy(alpha = 0.26f), RoundedCornerShape(10.dp))
                .border(1.dp, CalmGrayDim.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(Spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        style = CalmType.labelMd.copy(
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = CalmWhite,
                    )
                    Text(
                        text = if (apps.isEmpty()) "Choose apps for this group." else groupAppSummary(apps),
                        style = CalmType.labelMd.copy(fontSize = 10.sp, lineHeight = 14.sp),
                        color = CalmGray,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                AppGroupStatusChip(text = if (apps.isEmpty()) "NO APPS" else "${apps.size} APPS")
            }

            if (apps.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.stackSm),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(apps.take(4), key = { it.app.packageName }) { item ->
                        AppLabelChip(text = item.app.label)
                    }
                    if (apps.size > 4) {
                        item { AppLabelChip(text = "+${apps.size - 4} more") }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = formatGroupLimit(apps, group),
                            style = CalmType.headlineMd.copy(
                                fontSize = 24.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = CalmWhite,
                        )
                        Text(
                            text = if (limitedCount == 0) "Timer not set" else "Shared timer active",
                            style = CalmType.labelMd.copy(fontSize = 10.sp, lineHeight = 12.sp),
                            color = CalmGray,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    AppGroupStatusChip(text = if (limitedCount == 0) "LIMIT OFF" else "LIMIT ON")
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
                    CalmButton(
                        text = if (apps.isEmpty()) "ADD APPS" else "EDIT APPS",
                        style = CalmButtonStyle.Filled,
                        onClick = onAddApps,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CalmButton(
                        text = if (limitedCount == 0) "SET LIMIT" else "EDIT LIMIT",
                        style = CalmButtonStyle.Outlined,
                        onClick = onEditLimit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLabelChip(text: String) {
    Box(
        modifier = Modifier
            .background(CalmGrayDim.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .border(1.dp, CalmGrayDim.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = Spacing.stackSm, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = CalmType.labelMd.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CalmWhite,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppGroupStatusChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .background(CalmGrayDim.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .border(1.dp, CalmGrayDim.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
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
private fun AppLimitGroupEditorDialog(
    group: AppLimitGroup,
    allApps: List<AppLimitRowUiState>,
    selectedApps: List<AppLimitRowUiState>,
    onDismiss: () -> Unit,
    onSave: (packageNames: Set<String>, enabled: Boolean, minutes: Int) -> Unit,
) {
    val initialPackages = remember(group.id, selectedApps) {
        selectedApps.map { it.app.packageName }.toSet()
    }
    var selectedPackages by remember(group.id, initialPackages) { mutableStateOf(initialPackages) }
    var enabled by remember(group.id, selectedApps) {
        mutableStateOf(selectedApps.firstOrNull { it.rule != null }?.rule?.enabled ?: true)
    }
    var minutesText by remember(group.id, selectedApps) {
        mutableStateOf((groupLimitMinutes(selectedApps, group) ?: 30).toString())
    }
    var appQuery by remember(group.id) { mutableStateOf("") }
    val visibleApps = remember(allApps, appQuery) {
        val trimmed = appQuery.trim()
        if (trimmed.isBlank()) {
            allApps.sortedBy { it.app.label.lowercase(Locale.getDefault()) }
        } else {
            allApps.filter { item ->
                item.app.label.contains(trimmed, ignoreCase = true) ||
                    item.app.packageName.contains(trimmed, ignoreCase = true)
            }.sortedBy { it.app.label.lowercase(Locale.getDefault()) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = group.title,
                    style = CalmType.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = groupAppSummary(selectedApps),
                    style = CalmType.labelMd,
                    color = CalmGray,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.stackMd)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
                    AppGroupStatusChip(text = "${selectedPackages.size} SELECTED")
                    AppGroupStatusChip(text = formatGroupLimit(selectedApps, group))
                }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("Daily group limit (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                PresetLimits.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.base)) {
                        row.forEach { preset ->
                            CalmButton(
                                text = formatCompactLimit(preset),
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
                Text(
                    text = "Selected apps",
                    style = CalmType.labelMd,
                    color = CalmGray,
                )
                if (selectedPackages.isEmpty()) {
                    Text(
                        text = "No apps chosen yet.",
                        style = CalmType.labelMd,
                        color = CalmGray,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
                        items(selectedApps.filter { it.app.packageName in selectedPackages }, key = { it.app.packageName }) { item ->
                            AppLabelChip(text = item.app.label)
                        }
                    }
                }
                AppLimitSearchField(
                    query = appQuery,
                    onQueryChange = { appQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    contentPadding = PaddingValues(vertical = Spacing.stackSm),
                ) {
                    items(visibleApps, key = { it.app.packageName }) { item ->
                        AppPickerRow(
                            item = item,
                            checked = item.app.packageName in selectedPackages,
                            onCheckedChange = { checked ->
                                selectedPackages = if (checked) {
                                    selectedPackages + item.app.packageName
                                } else {
                                    selectedPackages - item.app.packageName
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            CalmButton(
                text = "Save group",
                style = CalmButtonStyle.Filled,
                enabled = selectedPackages.isNotEmpty(),
                onClick = { onSave(selectedPackages, enabled, minutesText.toIntOrNull() ?: 30) },
            )
        },
        dismissButton = {
            CalmButton(text = "Cancel", style = CalmButtonStyle.Outlined, onClick = onDismiss)
        },
    )
}

@Composable
private fun AppPickerRow(
    item: AppLimitRowUiState,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalmSurfaceContainer.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .border(1.dp, CalmGrayDim.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Spacing.stackSm, vertical = Spacing.stackSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.app.label, style = CalmType.bodyLg, color = CalmWhite, maxLines = 1)
            Text(text = item.app.category.label(), style = CalmType.labelMd, color = CalmGray, maxLines = 1)
        }
        Text(
            text = item.limitMinutes?.let { formatCompactLimit(it) } ?: "No limit",
            style = CalmType.labelMd,
            color = CalmGray,
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
            CalmButton(
                text = if (item.rule == null) "ADD LIMIT" else if (item.rule.enabled) "ON" else "OFF",
                style = CalmButtonStyle.Outlined,
                onClick = { onToggle(item.rule?.enabled?.not() ?: true) },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.stackSm),
            verticalArrangement = Arrangement.spacedBy(Spacing.base),
        ) {
            CalmButton(
                text = if (item.rule == null) "SET LIMIT" else "EDIT LIMIT",
                style = CalmButtonStyle.Outlined,
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            )
            if (item.rule != null) {
                CalmButton(
                    text = "REMOVE",
                    style = CalmButtonStyle.Outlined,
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ThinDivider(modifier = Modifier.padding(top = Spacing.stackMd))
    }
}

private fun formatGroupLimit(
    apps: List<AppLimitRowUiState>,
    group: AppLimitGroup,
): String {
    val limit = groupLimitMinutes(apps, group)
    return limit?.let { formatCompactLimit(it) } ?: "No limit set"
}

private fun groupLimitMinutes(
    apps: List<AppLimitRowUiState>,
    group: AppLimitGroup,
): Int? = apps.firstOrNull { it.rule?.enabled == true }?.limitMinutes ?: group.fallbackLimitMinutes

private fun groupAppSummary(apps: List<AppLimitRowUiState>): String {
    if (apps.isEmpty()) return "No apps selected"
    val labels = apps.take(3).joinToString(", ") { it.app.label }
    val remaining = apps.size - 3
    return if (remaining > 0) "$labels +$remaining more" else labels
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
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = item.app.label)
                Text(
                    text = item.app.packageName,
                    style = CalmType.labelMd,
                    color = CalmGray,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.stackSm)) {
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
