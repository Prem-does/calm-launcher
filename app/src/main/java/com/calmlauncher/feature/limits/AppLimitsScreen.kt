package com.calmlauncher.feature.limits

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppLimitCeilings
import com.calmlauncher.domain.model.AppLimitExtensionCaps
import com.calmlauncher.domain.model.OverrideDenialReason
import java.util.Locale

/** Daily limits the picker offers, in minutes. 0 blocks the app outright. */
private val PresetLimits = listOf(0, 15, 30, 45, 60, 120, 180)

/**
 * App Limits, stripped to the launcher's own vocabulary: flat rows on black, a hairline
 * between them, and no cards, chips or metric tiles. Groups are listed by name with their
 * shared timer as the row value; searching switches the list to individual apps. Both
 * editors open as full black screens rather than Material dialogs.
 */
@Composable
fun AppLimitsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLimitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val caps by viewModel.extensionCaps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var query by remember { mutableStateOf("") }

    // A refused extension and a deferred cap increase are both things the user has to be told
    // about explicitly: silently doing nothing is what made the old limit system feel broken.
    LaunchedEffect(viewModel) {
        viewModel.overrideDenied.collect { reason ->
            Toast.makeText(context, reason.message(), Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.capChangeDeferred.collect {
            Toast.makeText(
                context,
                "Raising a cap takes effect tomorrow, so it can't unblock an app right now.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    var editor by remember { mutableStateOf<AppLimitRowUiState?>(null) }
    var groupEditor by remember { mutableStateOf<AppLimitGroupUiState?>(null) }

    // Usage accrues while the user is inside the app they limited, so re-read it every time
    // this screen comes back to the foreground rather than trusting the construction-time read.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredApps = remember(state.apps, query) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            emptyList()
        } else {
            state.apps.filter { it.app.label.contains(trimmed, ignoreCase = true) }
        }
    }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "App Limits", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = Spacing.stackLg),
        ) {
            if (!state.usageAccessGranted) {
                item {
                    // Without Usage Access every figure below reads zero and no limit can
                    // ever fire, so say so rather than showing a convincing "0m used".
                    NoticeLine("Limits can't run without Usage Access. Every figure below reads zero.")
                    SettingRow(
                        title = "Grant Usage Access",
                        onClick = { openUsageAccessSettings(context) },
                        showChevron = true,
                    )
                }
            }

            if (!state.canShowBlockScreens) {
                item {
                    // Limits still fire without this; they just can't explain themselves. An
                    // app that runs out mid-scroll simply closes, which reads as a crash.
                    NoticeLine(
                        "Apps that run out of time are closed without warning. " +
                            "Allow block screens to see why first — and to add a few minutes.",
                    )
                    SettingRow(
                        title = "Allow block screens",
                        onClick = { openOverlaySettings(context) },
                        showChevron = true,
                    )
                }
            }

            item {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackMd),
                )
                ThinDivider()
            }

            if (query.isBlank()) {
                item {
                    SectionLabel("Today")
                    SettingRow(
                        title = "Used in limited apps",
                        value = formatDuration(state.limitedMinutesUsed),
                    )
                    SettingRow(
                        title = "Groups limited",
                        value = state.groupsLimited.toString(),
                    )
                    SectionLabel("Groups")
                }
                items(state.groups, key = { it.id }) { group ->
                    SettingRow(
                        title = group.title,
                        value = groupValue(group),
                        onClick = { groupEditor = group },
                    )
                }
                item {
                    Text(
                        text = "A group shares one daily timer across every app in it. " +
                            "Search above to limit a single app instead.",
                        style = CalmType.labelMd,
                        color = CalmGray,
                        modifier = Modifier.padding(
                            horizontal = Spacing.marginMobile,
                            vertical = Spacing.rowVertical,
                        ),
                    )

                    ExtensionBudgetSection(
                        caps = caps,
                        onChange = viewModel::setExtensionCaps,
                    )
                }
            } else {
                items(filteredApps, key = { it.app.packageName }) { row ->
                    SettingRow(
                        title = row.app.label,
                        value = limitStatusText(row),
                        onClick = { editor = row },
                    )
                }
                if (filteredApps.isEmpty()) {
                    item { NoticeLine("No apps found.") }
                }
            }
        }
    }

    editor?.let { row ->
        AppLimitEditor(
            item = row,
            onDismiss = { editor = null },
            onSave = { enabled, minutes ->
                viewModel.saveLimit(row.app.packageName, enabled, minutes)
                editor = null
            },
            onRemove = {
                viewModel.removeLimit(row.app.packageName)
                editor = null
            },
        )
    }

    groupEditor?.let { group ->
        AppLimitGroupEditor(
            group = group,
            allApps = state.apps,
            onDismiss = { groupEditor = null },
            onSave = { packageNames, enabled, minutes ->
                viewModel.saveGroupLimit(group.id, packageNames, enabled, minutes)
                groupEditor = null
            },
            onClear = {
                viewModel.clearGroupLimit(group)
                groupEditor = null
            },
        )
    }
}

/**
 * The daily extension budget: how many times an app may be extended past its limit, and how much
 * extra time that can add up to across the day.
 *
 * Both are enforced, and running out of either ends extensions for the day — which is the point.
 * Capping only the *count* is what allowed the original exploit: two extensions of unbounded length
 * is not a limit. The note about relaxations landing tomorrow is shown here rather than only in a
 * toast because it is the reason this screen cannot be used to get around a block in progress.
 */
@Composable
private fun ExtensionBudgetSection(
    caps: AppLimitExtensionCaps,
    onChange: (extensionsPerDay: Int, extraMinutesPerDay: Int) -> Unit,
) {
    SectionLabel("Extensions")
    SettingRow(
        title = "Per app, per day",
        value = if (caps.extensionsPerDay == 0) "Off" else caps.extensionsPerDay.toString(),
    )
    StepperRow(
        options = (0..AppLimitCeilings.MAX_EXTENSIONS_PER_DAY).toList(),
        selected = caps.extensionsPerDay,
        label = { if (it == 0) "Off" else it.toString() },
        onSelect = { onChange(it, caps.extraMinutesPerDay) },
    )
    Spacer(Modifier.height(Spacing.gutter))
    SettingRow(
        title = "Extra time per day",
        value = if (caps.extraMinutesPerDay == 0) "None" else "${caps.extraMinutesPerDay}m",
    )
    StepperRow(
        options = ExtraMinuteChoices,
        selected = caps.extraMinutesPerDay,
        label = { if (it == 0) "None" else "${it}m" },
        onSelect = { onChange(caps.extensionsPerDay, it) },
    )
    Text(
        text = "Lowering a cap applies straight away. Raising one waits until tomorrow, " +
            "so it can't be used to get back into an app that's already blocked.",
        style = CalmType.labelMd,
        color = CalmGray,
        modifier = Modifier.padding(
            horizontal = Spacing.marginMobile,
            vertical = Spacing.rowVertical,
        ),
    )
}

/** Extra-minute budgets on offer, all within [AppLimitCeilings.MAX_EXTRA_MINUTES_PER_DAY]. */
private val ExtraMinuteChoices = listOf(0, 10, 20, 30, 45, 60)

/** A row of mutually exclusive numeric choices; the active one inverts to solid white. */
@Composable
private fun <T> StepperRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base),
    ) {
        options.forEach { option ->
            val interaction = remember { MutableInteractionSource() }
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) CalmWhite else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(Spacing.base),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) CalmWhite else CalmGray,
                        shape = RoundedCornerShape(Spacing.base),
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = Spacing.stackMd),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = CalmType.labelMd,
                    color = if (isSelected) CalmBlack else CalmWhite,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Plain-language explanation of a refused extension. */
private fun OverrideDenialReason.message(): String = when (this) {
    OverrideDenialReason.NO_RULE -> "That app doesn't have a limit set."
    OverrideDenialReason.EXTENSIONS_EXHAUSTED -> "No extensions left for that app today."
    OverrideDenialReason.MINUTES_EXHAUSTED -> "Today's extra time is used up for that app."
    OverrideDenialReason.DISABLED -> "Extensions are turned off."
}

/** The value shown on a group row: the timer if one is set, plus how much of it is gone. */
private fun groupValue(group: AppLimitGroupUiState): String = when {
    !group.hasLimit -> if (group.apps.isEmpty()) "No apps" else "No limit"
    !group.limitEnabled -> "Paused"
    else -> {
        val remaining = group.remainingMinutes ?: 0
        if (remaining <= 0) "Reached" else "${formatDuration(remaining)} left"
    }
}

private fun limitStatusText(item: AppLimitRowUiState): String = when {
    item.rule == null -> "No limit"
    !item.rule.enabled -> "Paused"
    item.overrideActive -> "Extended"
    item.blockedToday -> "Blocked"
    else -> "${formatDuration(item.usedMinutes)} / ${formatDuration(item.limitMinutes ?: 0)}"
}

@Composable
private fun NoticeLine(text: String) {
    Text(
        text = text,
        style = CalmType.bodyMd,
        color = CalmGray,
        modifier = Modifier.padding(
            horizontal = Spacing.marginMobile,
            vertical = Spacing.rowVertical,
        ),
    )
}

/** A bare underlined query field; no container, no icon. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = CalmType.bodyLg.copy(color = CalmWhite),
        cursorBrush = SolidColor(CalmWhite),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            if (query.isBlank()) {
                Text(text = "Search apps", style = CalmType.bodyLg, color = CalmGray)
            }
            inner()
        },
    )
}

/**
 * Editing one group: the shared timer, an on/off row, and the app picker. Full-screen black
 * so the app list has room to breathe instead of scrolling inside a dialog.
 */
@Composable
private fun AppLimitGroupEditor(
    group: AppLimitGroupUiState,
    allApps: List<AppLimitRowUiState>,
    onDismiss: () -> Unit,
    onSave: (packageNames: Set<String>, enabled: Boolean, minutes: Int) -> Unit,
    onClear: () -> Unit,
) {
    var selectedPackages by remember(group.id) {
        mutableStateOf(group.apps.map { it.app.packageName }.toSet())
    }
    var enabled by remember(group.id) { mutableStateOf(group.limitEnabled || !group.hasLimit) }
    var minutes by remember(group.id) {
        mutableStateOf(group.limitMinutes ?: group.suggestedLimitMinutes)
    }
    var appQuery by remember(group.id) { mutableStateOf("") }

    val visibleApps = remember(allApps, appQuery) {
        val trimmed = appQuery.trim()
        allApps
            .filter { trimmed.isBlank() || it.app.label.contains(trimmed, ignoreCase = true) }
            .sortedBy { it.app.label.lowercase(Locale.getDefault()) }
    }

    FullScreenEditor(title = group.title, onDismiss = onDismiss) {
        item {
            SectionLabel("Daily limit")
            LimitPicker(selected = minutes, onSelect = { minutes = it })
            SettingRow(
                title = if (enabled) "Limit on" else "Limit off",
                onClick = { enabled = !enabled },
            )
            SectionLabel("Apps · ${selectedPackages.size} selected")
            SearchField(
                query = appQuery,
                onQueryChange = { appQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackMd),
            )
            ThinDivider()
        }

        items(visibleApps, key = { it.app.packageName }) { item ->
            SelectableAppRow(
                label = item.app.label,
                detail = formatDuration(item.usedMinutes) + " today",
                selected = item.app.packageName in selectedPackages,
                onToggle = {
                    selectedPackages = if (item.app.packageName in selectedPackages) {
                        selectedPackages - item.app.packageName
                    } else {
                        selectedPackages + item.app.packageName
                    }
                },
            )
        }

        item {
            Text(
                text = "Unchecking an app removes its limit entirely.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.rowVertical,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.marginMobile),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            ) {
                CalmButton(
                    text = "SAVE",
                    style = CalmButtonStyle.Filled,
                    enabled = selectedPackages.isNotEmpty(),
                    onClick = { onSave(selectedPackages, enabled, minutes) },
                )
                if (group.hasLimit) {
                    CalmButton(text = "REMOVE LIMIT", style = CalmButtonStyle.Text, onClick = onClear)
                }
            }
        }
    }
}

/** Editing a single app's limit. */
@Composable
private fun AppLimitEditor(
    item: AppLimitRowUiState,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, minutes: Int) -> Unit,
    onRemove: () -> Unit,
) {
    var enabled by remember(item.app.packageName) { mutableStateOf(item.rule?.enabled ?: true) }
    var minutes by remember(item.app.packageName) { mutableStateOf(item.limitMinutes ?: 30) }

    FullScreenEditor(title = item.app.label, onDismiss = onDismiss) {
        item {
            SectionLabel("Today")
            SettingRow(title = "Used", value = formatDuration(item.usedMinutes))

            SectionLabel("Daily limit")
            LimitPicker(selected = minutes, onSelect = { minutes = it })
            Text(
                text = "0m blocks this app immediately — no daily allowance at all.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.rowVertical,
                ),
            )
            SettingRow(
                title = if (enabled) "Limit on" else "Limit off",
                onClick = { enabled = !enabled },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.marginMobile),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            ) {
                CalmButton(
                    text = "SAVE",
                    style = CalmButtonStyle.Filled,
                    onClick = { onSave(enabled, minutes) },
                )
                if (item.rule != null) {
                    CalmButton(text = "REMOVE", style = CalmButtonStyle.Text, onClick = onRemove)
                }
            }
        }
    }
}

/**
 * A black full-screen surface with a back bar, hosting a [LazyColumn] of editor content.
 * Both limit editors use it so they read as screens, not as popups over a screen.
 */
@Composable
private fun FullScreenEditor(
    title: String,
    onDismiss: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CalmBlack)
                // CalmBackBar applies the status-bar inset itself; only the bottom is left.
                .navigationBarsPadding(),
        ) {
            CalmBackBar(title = title, onBack = onDismiss)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.stackLg),
                content = content,
            )
        }
    }
}

/** The daily-limit presets as a wrapped row of tappable values; selected inverts. */
@Composable
private fun LimitPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        PresetLimits.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { preset ->
                    LimitChoice(
                        label = formatDuration(preset),
                        selected = preset == selected,
                        onClick = { onSelect(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep the last row's cells the same width as the full rows above it.
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        ThinDivider()
    }
}

@Composable
private fun LimitChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = CalmType.bodyMd,
        color = if (selected) CalmBlack else CalmWhite,
        textAlign = TextAlign.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(if (selected) CalmWhite else CalmBlack)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = Spacing.stackMd),
    )
}

/** An app row with a 1dp square that fills solid when the app is in the group. */
@Composable
private fun SelectableAppRow(
    label: String,
    detail: String,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, if (selected) CalmWhite else CalmGray)
                    .background(if (selected) CalmWhite else CalmBlack),
            )
            Text(
                text = label,
                style = CalmType.bodyLg,
                color = CalmWhite,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(text = detail, style = CalmType.labelMd, color = CalmGray)
        }
        ThinDivider()
    }
}

private fun openUsageAccessSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** "Display over other apps", scoped to Calm — the grant that lets a block screen be drawn. */
private fun openOverlaySettings(context: android.content.Context) {
    val scoped = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        .setData(android.net.Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Some OEM builds reject the package-scoped form; fall back to the full list.
    if (runCatching { context.startActivity(scoped) }.isFailure) {
        val list = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(list) }
    }
}

/** "0m" / "45m" / "2h" / "1h 30m". */
private fun formatDuration(minutes: Int): String = when {
    minutes <= 0 -> "0m"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
