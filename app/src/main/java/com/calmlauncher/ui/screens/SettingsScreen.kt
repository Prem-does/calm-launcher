package com.calmlauncher.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.shouldQuietInteractions
import com.calmlauncher.security.DevicePolicyEnforcer
import com.calmlauncher.ui.theme.LocalLauncherSettings
import com.calmlauncher.ui.components.CalmButton
import com.calmlauncher.ui.components.LauncherChrome
import com.calmlauncher.data.db.CalmDatabaseProvider
import com.calmlauncher.data.system.LauncherAppCatalog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateListOf
import com.calmlauncher.data.db.entity.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.collectAsState

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    state: LauncherSettingsState,
    todayScreenTimeMinutes: Int,
    calmInsight: String,
    onSetPin: (String) -> Unit,
    onUnlock: (String) -> Unit,
    onLock: () -> Unit,
    onToggleGrayscale: () -> Unit,
    onToggleKioskMode: () -> Unit,
    onToggleHiddenStatusBar: () -> Unit,
    onSetPreference: (String, String) -> Unit,
    onTogglePreference: (String) -> Unit,
    onToggleFocusMode: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf("unlock") }
    var showDeviceOwnerDialog by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val settings = LocalLauncherSettings.current
    var adminActive by remember { mutableStateOf(DevicePolicyEnforcer.isAdminActive(context)) }
    var deviceOwner by remember { mutableStateOf(DevicePolicyEnforcer.isDeviceOwner(context)) }

    fun persistedValue(key: String, defaultValue: String): String = state.preferences[key] ?: defaultValue
    fun persistedEnabled(key: String, defaultValue: Boolean): Boolean = state.preferences[key]?.let { it.equals("ON", ignoreCase = true) || it.equals("true", ignoreCase = true) } ?: defaultValue

    val toggleState = remember(state.preferences) {
        mutableStateMapOf(
            "Enforcement" to persistedEnabled("Enforcement", false),
            "Show Time" to persistedEnabled("Show Time", true),
            "Show Date" to persistedEnabled("Show Date", true),
            "Show Battery %" to persistedEnabled("Show Battery %", true),
            "Show Weather" to persistedEnabled("Show Weather", false),
            "Show Next Event" to persistedEnabled("Show Next Event", false),
            "Center Align Layout" to persistedEnabled("Center Align Layout", true),
            "Minimal Clock Style" to persistedEnabled("Minimal Clock Style", true),
            "Large Clock Style" to persistedEnabled("Large Clock Style", false),
            "Hide Search Bar" to persistedEnabled("Hide Search Bar", false),
            "Hide Navigation Bar" to persistedEnabled("Hide Navigation Bar", false),
            "Disable Widgets" to persistedEnabled("Disable Widgets", true),
            "Allowed Apps List" to persistedEnabled("Allowed Apps List", true),
            "Hidden Apps" to persistedEnabled("Hidden Apps", false),
            "Lock Apps" to persistedEnabled("Lock Apps", true),
            "Focus Mode Apps" to persistedEnabled("Focus Mode Apps", true),
            "Essential Apps Only" to persistedEnabled("Essential Apps Only", true),
            "Block App Installation" to persistedEnabled("Block App Installation", true),
            "Disable App Suggestions" to persistedEnabled("Disable App Suggestions", true),
            "Disable Recently Used Apps" to persistedEnabled("Disable Recently Used Apps", true),
            "Require Intent Before Opening" to persistedEnabled("Require Intent Before Opening", true),
            "Require Long Press" to persistedEnabled("Require Long Press", true),
            "Require PIN For Apps" to persistedEnabled("Require PIN For Apps", false),
            "Daily Usage Limits" to persistedEnabled("Daily Usage Limits", false),
            "App Cooldown Timer" to persistedEnabled("App Cooldown Timer", false),
            "App Timeout Auto-Close" to persistedEnabled("App Timeout Auto-Close", false),
            "Slow Mode" to persistedEnabled("Slow Mode", false),
            "Dead-End Feeds" to persistedEnabled("Dead-End Feeds", false),
            "Enable Focus Mode" to persistedEnabled("Enable Focus Mode", true),
            "Schedule Focus Mode" to persistedEnabled("Schedule Focus Mode", false),
            "Emergency Bypass" to persistedEnabled("Emergency Bypass", true),
            "Block Browser" to persistedEnabled("Block Browser", true),
            "Block Social Media" to persistedEnabled("Block Social Media", true),
            "Block Play Store" to persistedEnabled("Block Play Store", true),
            "Hide Notifications" to persistedEnabled("Hide Notifications", true),
            "Lock Quick Settings" to persistedEnabled("Lock Quick Settings", true),
            "Ultra Minimal Screen" to persistedEnabled("Ultra Minimal Screen", true),
            "Allow Calls Only" to persistedEnabled("Allow Calls Only", false),
            "Allow Messages Only" to persistedEnabled("Allow Messages Only", false),
            "Silent Notifications" to persistedEnabled("Silent Notifications", true),
            "Hide Notification Icons" to persistedEnabled("Hide Notification Icons", true),
            "Disable Notification Badges" to persistedEnabled("Disable Notification Badges", true),
            "Disable Popups" to persistedEnabled("Disable Popups", true),
            "Disable Vibration" to persistedEnabled("Disable Vibration", true),
            "Screen Time Counter" to persistedEnabled("Screen Time Counter", true),
            "Unlock Counter" to persistedEnabled("Unlock Counter", false),
            "Focus Hours Tracker" to persistedEnabled("Focus Hours Tracker", true),
            "Daily Usage Graph" to persistedEnabled("Daily Usage Graph", false),
            "App Usage Statistics" to persistedEnabled("App Usage Statistics", true),
            "Relapse Risk Detection" to persistedEnabled("Relapse Risk Detection", false),
            "Dopamine Detox Mode" to persistedEnabled("Dopamine Detox Mode", true),
            "Weekly Usage Report" to persistedEnabled("Weekly Usage Report", true),
            "Disable Internet Access" to persistedEnabled("Disable Internet Access", false),
            "Black & White Only" to persistedEnabled("Black & White Only", true),
            "Disable Entertainment Apps" to persistedEnabled("Disable Entertainment Apps", true),
            "Hide All Non-Essential Apps" to persistedEnabled("Hide All Non-Essential Apps", true),
            "Lock Device During Focus Sessions" to persistedEnabled("Lock Device During Focus Sessions", false),
            "Calm Unlock Screen" to persistedEnabled("Calm Unlock Screen", true),
            "Breathing Unlock Animation" to persistedEnabled("Breathing Unlock Animation", false),
            "Slow Unlock Delay" to persistedEnabled("Slow Unlock Delay", true),
            "Text-Only App Search" to persistedEnabled("Text-Only App Search", true),
            "Disable App Icons" to persistedEnabled("Disable App Icons", true),
            "Gesture Navigation" to persistedEnabled("Gesture Navigation", true),
            "Swipe Down For Search" to persistedEnabled("Swipe Down For Search", true),
            "Swipe Left For Notes" to persistedEnabled("Swipe Left For Notes", true),
            "Swipe Right For Focus Mode" to persistedEnabled("Swipe Right For Focus Mode", true),
            "Disable Infinite Scrolling" to persistedEnabled("Disable Infinite Scrolling", true),
            "Vertical List Navigation" to persistedEnabled("Vertical List Navigation", true),
            "Silent Mode Default" to persistedEnabled("Silent Mode Default", true),
            "Minimal Notification Sounds" to persistedEnabled("Minimal Notification Sounds", true),
            "Focus Sounds" to persistedEnabled("Focus Sounds", false),
            "Meditation Ambient Sounds" to persistedEnabled("Meditation Ambient Sounds", false),
            "Keyboard Sound Off" to persistedEnabled("Keyboard Sound Off", true),
            "Night Schedule" to persistedEnabled("Night Schedule", false),
            "Extra Dim Display" to persistedEnabled("Extra Dim Display", false),
            "Warm Grayscale" to persistedEnabled("Warm Grayscale", false),
            "Disable Internet At Night" to persistedEnabled("Disable Internet At Night", false),
            "Sleep Reminder" to persistedEnabled("Sleep Reminder", false),
            "Bedtime Lock" to persistedEnabled("Bedtime Lock", false),
            "Disable Brightness Boost" to persistedEnabled("Disable Brightness Boost", true),
            "PIN Protect Settings" to persistedEnabled("PIN Protect Settings", true),
            "Prevent Launcher Exit" to persistedEnabled("Prevent Launcher Exit", true),
            "Set As Permanent Home App" to persistedEnabled("Set As Permanent Home App", true),
            "Kiosk Mode" to persistedEnabled("Kiosk Mode", false),
            "Lock Safe Mode Access" to persistedEnabled("Lock Safe Mode Access", true),
            "Hide Sensitive Apps" to persistedEnabled("Hide Sensitive Apps", true),
            "Emergency Contact Access" to persistedEnabled("Emergency Contact Access", true),
            "Breathing Exercise Screen" to persistedEnabled("Breathing Exercise Screen", true),
            "Focus Timer" to persistedEnabled("Focus Timer", true),
            "Pomodoro Mode" to persistedEnabled("Pomodoro Mode", false),
            "Meditation Timer" to persistedEnabled("Meditation Timer", false),
            "Calm Quotes" to persistedEnabled("Calm Quotes", true),
            "Blank Screen Mode" to persistedEnabled("Blank Screen Mode", false),
            "Ambient Rain Sounds" to persistedEnabled("Ambient Rain Sounds", false),
            "Deep Work Session Timer" to persistedEnabled("Deep Work Session Timer", true),
            "Intent-Based App Opening" to persistedEnabled("Intent-Based App Opening", true),
            "Dopamine Detection Engine" to persistedEnabled("Dopamine Detection Engine", true),
            "One-App-At-A-Time Mode" to persistedEnabled("One-App-At-A-Time Mode", false),
            "Analog Mode" to persistedEnabled("Analog Mode", false),
            "Dynamic Minimalism" to persistedEnabled("Dynamic Minimalism", false),
            "Recovery Mode" to persistedEnabled("Recovery Mode", false),
            "E-Ink Simulation" to persistedEnabled("E-Ink Simulation", false),
            "Breath Unlock" to persistedEnabled("Breath Unlock", false),
            "Invisible Social Apps" to persistedEnabled("Invisible Social Apps", true),
            "Minimal Social Layer" to persistedEnabled("Minimal Social Layer", true),
            "Reward Real Life" to persistedEnabled("Reward Real Life", true),
            "Calm AI Assistant" to persistedEnabled("Calm AI Assistant", true),
            "Usage Reflection Screen" to persistedEnabled("Usage Reflection Screen", true)
        )
    }

    // Telemetry flow
    val telemetryFlow = remember { CalmDatabaseProvider.get(context).telemetryDao().observeRecent(50) }
    val telemetryList by telemetryFlow.collectAsState(initial = emptyList())
    var showTelemetryDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }

    val choiceState = remember(state.preferences) {
        mutableStateMapOf(
            "Theme" to persistedValue("Theme", "Pure Black"),
            "Grayscale Mode" to persistedValue("Grayscale Mode", "Always On"),
            "Font Style" to persistedValue("Font Style", "Inter"),
            "Font Size" to persistedValue("Font Size", "Medium"),
            "UI Density" to persistedValue("UI Density", "Minimal"),
            "Animation Level" to persistedValue("Animation Level", "Soft Fade"),
            "Status Bar" to persistedValue("Status Bar", "Show"),
            "Wallpaper" to persistedValue("Wallpaper", "Solid Black"),
            "App Opening Delay" to persistedValue("App Opening Delay", "3 sec"),
            "Notification Mode" to persistedValue("Notification Mode", "Disable All"),
            "Notification Digest Time" to persistedValue("Notification Digest Time", "21:00"),
            "Daily Phone Limit" to persistedValue("Daily Phone Limit", "120 min"),
            "Alarm Sound Style" to persistedValue("Alarm Sound Style", "Minimal"),
            "Environment Mode" to persistedValue("Environment Mode", "Study"),
            "Friction Level" to persistedValue("Friction Level", "Light")
        )
    }

    @Composable
    fun sectionHeader(label: String) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(label, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFF2A2A2A))
        Spacer(modifier = Modifier.height(10.dp))
    }

    @Composable
    fun checkboxRow(label: String, map: MutableMap<String, Boolean>) {
        val current = map[label] ?: false
        var showBlockConfirm by remember { mutableStateOf(false) }
        SettingRow(label = label, value = if (current) "ON" else "OFF", onClick = {
            val newVal = !current
            map[label] = newVal
            onTogglePreference(label)
            if (label == "Enable Focus Mode") {
                onToggleFocusMode()
            }
            // For critical blocking toggles, require device-admin consent if not active.
            val critical = listOf("Disable Internet Access", "Block Browser", "Block Play Store", "Block Social Media", "Disable Entertainment Apps")
            if (newVal && critical.contains(label) && !DevicePolicyEnforcer.isAdminActive(context)) {
                showBlockConfirm = true
            }
        })

        if (showBlockConfirm) {
            AlertDialog(
                onDismissRequest = { showBlockConfirm = false },
                confirmButton = {
                    CalmButton(text = "ENABLE DEVICE ADMIN", onClick = {
                        context.startActivity(
                            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DevicePolicyEnforcer.adminComponent(context))
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Device admin is required to fully block apps and network access.")
                            }
                        )
                        showBlockConfirm = false
                    })
                },
                dismissButton = {
                    CalmButton(text = "CANCEL", onClick = {
                        // revert toggle
                        map[label] = false
                        onTogglePreference(label)
                        showBlockConfirm = false
                    })
                },
                title = { Text("Enable blocking?") },
                text = { Text("This action requires Device Admin privileges to reliably block apps or network access. Enable Device Admin now?") }
            )
        }
    }

    @Composable
    fun toggleRow(label: String, current: Boolean, map: MutableMap<String, Boolean>, onChange: () -> Unit) {
        SettingRow(label = label, value = if (current) "ON" else "OFF", onClick = {
            map[label] = !current
            onChange()
            onTogglePreference(label)
        })
    }

    @Composable
    fun choiceRow(label: String, value: String, options: List<String>, map: MutableMap<String, String>) {
        SettingRow(label = label, value = value, onClick = {
            val next = nextOption(value, options)
            map[label] = next
            onSetPreference(label, next)
            if (label == "Status Bar") {
                onToggleHiddenStatusBar()
            }
            if (label == "Grayscale Mode") {
                onToggleGrayscale()
            }
        })
    }

    @Composable
    fun SettingRow(label: String, value: String, onClick: () -> Unit) {
        val clickHandler = remember(settings.shouldQuietInteractions(), onClick) {
            {
                if (!settings.shouldQuietInteractions()) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = clickHandler)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Text(value, color = Color.White.copy(alpha = 0.72f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider(color = Color(0xFF2A2A2A))
        }
    }

    fun nextOption(current: String, options: List<String>): String {
        if (options.isEmpty()) return current
        val index = options.indexOf(current)
        return options[(if (index < 0) 0 else index + 1) % options.size]
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            confirmButton = {
                CalmButton(
                    text = if (dialogMode == "set") "SAVE PIN" else "UNLOCK",
                    onClick = {
                        if (dialogMode == "set") onSetPin(pinInput) else onUnlock(pinInput)
                        pinInput = ""
                        showPinDialog = false
                    }
                )
            },
            dismissButton = { CalmButton(text = "CANCEL", onClick = { showPinDialog = false }) },
            title = { Text(if (dialogMode == "set") "Set PIN" else "Unlock Settings") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    label = { Text("PIN") }
                )
            }
        )
    }

    LauncherChrome(
        statusText = "12:45 • MON OCT 23 • 85%",
        rightActions = listOf("MENU", "LOG"),
        bottomActions = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("SETTINGS MENU", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("TODAY ${todayScreenTimeMinutes} MIN", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(18.dp))

            if (!state.settingsUnlocked && state.pinHash != null) {
                CalmButton(text = "UNLOCK SETTINGS", onClick = {
                    dialogMode = "unlock"
                    showPinDialog = true
                })
                Spacer(modifier = Modifier.height(14.dp))
            }

            sectionHeader("ENFORCEMENT")
            checkboxRow("Enforcement", toggleState)
            Text(
                if (toggleState["Enforcement"] == true)
                    "On. App hiding, blocking, delays, and locks apply per the settings below."
                else
                    "Off. The launcher stays minimal but restricts nothing. Turn on to activate hiding, blocking, delays, and locks.",
                color = Color.White.copy(alpha = 0.5f),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
            )

            sectionHeader("APPEARANCE")
            choiceRow("Theme", choiceState["Theme"] ?: "Pure Black", listOf("Pure Black", "Dark Gray", "Paper White"), choiceState)
            choiceRow("Grayscale Mode", choiceState["Grayscale Mode"] ?: "Always On", listOf("Always On", "Scheduled", "Off"), choiceState)
            choiceRow("Font Style", choiceState["Font Style"] ?: "Inter", listOf("Inter", "IBM Plex Sans", "Helvetica Neue"), choiceState)
            choiceRow("Font Size", choiceState["Font Size"] ?: "Medium", listOf("Small", "Medium", "Large"), choiceState)
            choiceRow("UI Density", choiceState["UI Density"] ?: "Minimal", listOf("Compact", "Comfortable", "Minimal"), choiceState)
            choiceRow("Animation Level", choiceState["Animation Level"] ?: "Soft Fade", listOf("None", "Soft Fade", "Slow Fade"), choiceState)
            choiceRow("Status Bar", choiceState["Status Bar"] ?: "Show", listOf("Show", "Hide"), choiceState)
            choiceRow("Wallpaper", choiceState["Wallpaper"] ?: "Solid Black", listOf("Solid Black", "Solid White", "Custom Minimal"), choiceState)

            sectionHeader("HOME SCREEN")
            checkboxRow("Show Time", toggleState)
            checkboxRow("Show Date", toggleState)
            checkboxRow("Show Battery %", toggleState)
            checkboxRow("Show Weather", toggleState)
            checkboxRow("Show Next Event", toggleState)
            checkboxRow("Center Align Layout", toggleState)
            checkboxRow("Minimal Clock Style", toggleState)
            checkboxRow("Large Clock Style", toggleState)
            checkboxRow("Hide Search Bar", toggleState)
            checkboxRow("Hide Navigation Bar", toggleState)
            checkboxRow("Disable Widgets", toggleState)
            checkboxRow("Show Icons On Home", toggleState)
            checkboxRow("Compact Home", toggleState)

            CalmButton(text = "EDIT HOME FAVORITES", onClick = { showFavoritesDialog = true })
            if (showFavoritesDialog) {
                val apps = LauncherAppCatalog.loadSearchApps(context, state).sortedBy { it.label }
                val initial = state.preferences["Home Favorites"]?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                val selected = remember(state.preferences) { mutableStateListOf<String>().apply { addAll(initial) } }

                AlertDialog(
                    onDismissRequest = { showFavoritesDialog = false },
                    confirmButton = {
                        CalmButton(text = "SAVE", onClick = {
                            onSetPreference("Home Favorites", selected.joinToString(","))
                            showFavoritesDialog = false
                        })
                    },
                    dismissButton = {
                        CalmButton(text = "CANCEL", onClick = { showFavoritesDialog = false })
                    },
                    title = { Text("Edit Home Favorites") },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(apps) { app ->
                                val pkg = app.packageName
                                val checked = selected.contains(pkg)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { if (checked) selected.remove(pkg) else selected.add(pkg) }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(app.label, color = Color.White)
                                    Checkbox(checked = checked, onCheckedChange = { now -> if (now) selected.add(pkg) else selected.remove(pkg) })
                                }
                            }
                        }
                    }
                )
            }

            sectionHeader("APP MANAGEMENT")
            checkboxRow("Allowed Apps List", toggleState)
            checkboxRow("Hidden Apps", toggleState)
            checkboxRow("Lock Apps", toggleState)
            checkboxRow("Focus Mode Apps", toggleState)
            checkboxRow("Essential Apps Only", toggleState)
            checkboxRow("Block App Installation", toggleState)
            checkboxRow("Disable App Suggestions", toggleState)
            checkboxRow("Disable Recently Used Apps", toggleState)

            sectionHeader("APP OPENING CONTROLS")
            choiceRow("App Opening Delay", choiceState["App Opening Delay"] ?: "3 sec", listOf("Off", "3 sec", "5 sec", "10 sec"), choiceState)
            checkboxRow("Slow Mode", toggleState)
            checkboxRow("Dead-End Feeds", toggleState)
            checkboxRow("Require Intent Before Opening", toggleState)
            checkboxRow("Require Long Press", toggleState)
            checkboxRow("Require PIN For Apps", toggleState)
            checkboxRow("Daily Usage Limits", toggleState)
            checkboxRow("App Cooldown Timer", toggleState)
            checkboxRow("App Timeout Auto-Close", toggleState)

            sectionHeader("FOCUS MODE")
            checkboxRow("Enable Focus Mode", toggleState)
            checkboxRow("Schedule Focus Mode", toggleState)
            checkboxRow("Emergency Bypass", toggleState)
            checkboxRow("Block Browser", toggleState)
            checkboxRow("Block Social Media", toggleState)
            checkboxRow("Block Play Store", toggleState)
            checkboxRow("Hide Notifications", toggleState)
            checkboxRow("Lock Quick Settings", toggleState)
            checkboxRow("Ultra Minimal Screen", toggleState)
            checkboxRow("Allow Calls Only", toggleState)
            checkboxRow("Allow Messages Only", toggleState)

            sectionHeader("NOTIFICATIONS")
            choiceRow("Notification Mode", choiceState["Notification Mode"] ?: "Disable All", listOf("Disable All", "Allow Calls", "Allow Messages", "Allow OTP Messages"), choiceState)
            checkboxRow("Silent Notifications", toggleState)
            checkboxRow("Hide Notification Icons", toggleState)
            checkboxRow("Disable Notification Badges", toggleState)
            checkboxRow("Disable Popups", toggleState)
            checkboxRow("Disable Vibration", toggleState)
            choiceRow("Notification Digest Time", choiceState["Notification Digest Time"] ?: "21:00", listOf("21:00", "20:00", "22:00"), choiceState)

            sectionHeader("DIGITAL WELLBEING")
            checkboxRow("Screen Time Counter", toggleState)
            checkboxRow("Unlock Counter", toggleState)
            checkboxRow("Focus Hours Tracker", toggleState)
            checkboxRow("Daily Usage Graph", toggleState)
            checkboxRow("App Usage Statistics", toggleState)
            checkboxRow("Relapse Risk Detection", toggleState)
            checkboxRow("Dopamine Detox Mode", toggleState)
            choiceRow("Daily Phone Limit", choiceState["Daily Phone Limit"] ?: "120 min", listOf("60 min", "120 min", "180 min", "Unlimited"), choiceState)
            checkboxRow("Weekly Usage Report", toggleState)

            sectionHeader("PRIVACY & TELEMETRY")
            checkboxRow("Telemetry Consent", toggleState)
            if (toggleState["Telemetry Consent"] == true) {
                CalmButton(text = "VIEW TELEMETRY LOG", onClick = { showTelemetryDialog = true })
            }

            sectionHeader("FOCUS TOOLS")
            checkboxRow("Disable Internet Access", toggleState)
            checkboxRow("Black & White Only", toggleState)
            checkboxRow("Disable Entertainment Apps", toggleState)
            checkboxRow("Hide All Non-Essential Apps", toggleState)
            checkboxRow("Lock Device During Focus Sessions", toggleState)
            checkboxRow("Calm Unlock Screen", toggleState)
            checkboxRow("Breathing Unlock Animation", toggleState)
            checkboxRow("Slow Unlock Delay", toggleState)

            sectionHeader("SEARCH & NAVIGATION")
            checkboxRow("Text-Only App Search", toggleState)
            checkboxRow("Disable App Icons", toggleState)
            checkboxRow("Gesture Navigation", toggleState)
            checkboxRow("Swipe Down For Search", toggleState)
            checkboxRow("Swipe Left For Notes", toggleState)
            checkboxRow("Swipe Right For Focus Mode", toggleState)
            checkboxRow("Disable Infinite Scrolling", toggleState)
            checkboxRow("Vertical List Navigation", toggleState)

            sectionHeader("SOUND")
            checkboxRow("Silent Mode Default", toggleState)
            checkboxRow("Minimal Notification Sounds", toggleState)
            checkboxRow("Focus Sounds", toggleState)
            checkboxRow("Meditation Ambient Sounds", toggleState)
            checkboxRow("Keyboard Sound Off", toggleState)
            checkboxRow("Vibration Strength", toggleState)
            choiceRow("Alarm Sound Style", choiceState["Alarm Sound Style"] ?: "Minimal", listOf("Minimal", "Soft", "Silent"), choiceState)

            sectionHeader("NIGHT MODE")
            checkboxRow("Night Schedule", toggleState)
            checkboxRow("Extra Dim Display", toggleState)
            checkboxRow("Warm Grayscale", toggleState)
            checkboxRow("Disable Internet At Night", toggleState)
            checkboxRow("Sleep Reminder", toggleState)
            checkboxRow("Bedtime Lock", toggleState)
            checkboxRow("Disable Brightness Boost", toggleState)

            sectionHeader("SECURITY")
            CalmButton(text = "ENABLE DEVICE ADMIN", onClick = {
                context.startActivity(
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DevicePolicyEnforcer.adminComponent(context))
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable stricter focus rules for installs, quick settings, and launcher exit.")
                    }
                )
            })
            Spacer(modifier = Modifier.height(8.dp))
            SettingRow(label = "Device Admin", value = if (adminActive) "ACTIVE" else "INACTIVE", onClick = {})
            SettingRow(label = "Device Owner", value = if (deviceOwner) "YES" else "NO", onClick = {})
            CalmButton(text = "REFRESH ADMIN STATUS", onClick = {
                adminActive = DevicePolicyEnforcer.isAdminActive(context)
                deviceOwner = DevicePolicyEnforcer.isDeviceOwner(context)
            })
            Spacer(modifier = Modifier.height(8.dp))
            CalmButton(text = "REAPPLY POLICIES", onClick = { DevicePolicyEnforcer.apply(context, state) })
            Spacer(modifier = Modifier.height(8.dp))
            CalmButton(text = "DEVICE OWNER (ADB)", onClick = { showDeviceOwnerDialog = true })
            if (showDeviceOwnerDialog) {
                AlertDialog(
                    onDismissRequest = { showDeviceOwnerDialog = false },
                    confirmButton = {
                        CalmButton(text = "CLOSE", onClick = { showDeviceOwnerDialog = false })
                    },
                    title = { Text("Device Owner Instructions") },
                    text = {
                        Text("To enable full device-owner features (requires adb provisioning), run:\n\nadb shell dpm set-device-owner ${context.packageName}/.security.CalmDeviceAdminReceiver\n\nThis must be executed on a freshly provisioned device or via provisioning tools.")
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CalmButton(text = "OPEN ACCESSIBILITY", onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            Spacer(modifier = Modifier.height(8.dp))
            CalmButton(text = "OPEN NOTIFICATION ACCESS", onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
            Spacer(modifier = Modifier.height(12.dp))
            checkboxRow("PIN Protect Settings", toggleState)
            checkboxRow("Prevent Launcher Exit", toggleState)
            checkboxRow("Set As Permanent Home App", toggleState)
            toggleRow("Kiosk Mode", state.kioskModeEnabled, toggleState) { onToggleKioskMode() }
            checkboxRow("Lock Safe Mode Access", toggleState)
            checkboxRow("Hide Sensitive Apps", toggleState)
            checkboxRow("Emergency Contact Access", toggleState)

            sectionHeader("MEDITATION TOOLS")
            checkboxRow("Breathing Exercise Screen", toggleState)
            checkboxRow("Focus Timer", toggleState)
            checkboxRow("Pomodoro Mode", toggleState)
            checkboxRow("Meditation Timer", toggleState)
            checkboxRow("Calm Quotes", toggleState)
            checkboxRow("Blank Screen Mode", toggleState)
            checkboxRow("Ambient Rain Sounds", toggleState)
            checkboxRow("Deep Work Session Timer", toggleState)

            sectionHeader("MODES")
            choiceRow("Environment Mode", choiceState["Environment Mode"] ?: "Study", listOf("Study", "Sleep", "Gym", "Deep Work", "Drive", "Minimal", "Recovery", "Nature", "Social"), choiceState)
            choiceRow("Friction Level", choiceState["Friction Level"] ?: "Light", listOf("Light", "Monk", "Hardcore Monk"), choiceState)
            checkboxRow("Intent-Based App Opening", toggleState)
            checkboxRow("Dopamine Detection Engine", toggleState)
            checkboxRow("One-App-At-A-Time Mode", toggleState)
            checkboxRow("Analog Mode", toggleState)
            checkboxRow("Dynamic Minimalism", toggleState)
            checkboxRow("Recovery Mode", toggleState)
            checkboxRow("E-Ink Simulation", toggleState)
            checkboxRow("Breath Unlock", toggleState)
            checkboxRow("Invisible Social Apps", toggleState)
            checkboxRow("Minimal Social Layer", toggleState)
            checkboxRow("Reward Real Life", toggleState)
            checkboxRow("Calm AI Assistant", toggleState)
            checkboxRow("Usage Reflection Screen", toggleState)

            if (calmInsight.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text("WEEKLY PATTERN", color = Color.White.copy(alpha = 0.38f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(calmInsight, color = Color.White.copy(alpha = 0.72f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Did your phone help you today?", color = Color.White.copy(alpha = 0.38f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(18.dp))
            CalmButton(text = if (state.settingsUnlocked) "LOCK SETTINGS" else "UNLOCK SETTINGS", onClick = {
                if (state.pinHash == null) {
                    dialogMode = "set"
                    showPinDialog = true
                } else if (state.settingsUnlocked) {
                    onLock()
                } else {
                    dialogMode = "unlock"
                    showPinDialog = true
                }
            })
            Spacer(modifier = Modifier.height(12.dp))
            if (showTelemetryDialog) {
                AlertDialog(
                    onDismissRequest = { showTelemetryDialog = false },
                    confirmButton = { CalmButton(text = "CLOSE", onClick = { showTelemetryDialog = false }) },
                    title = { Text("Telemetry Log (recent)") },
                    text = {
                        Column { 
                            telemetryList.take(50).forEach { e ->
                                Text("${e.type}: ${e.details}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                )
            }
            CalmButton(text = "BACK", onClick = onBack)
        }
    }
}

@Composable
private fun sectionHeader(label: String) {
    Spacer(modifier = Modifier.height(18.dp))
    Text(label, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = Color(0xFF2A2A2A))
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun checkboxRow(label: String, toggleState: MutableMap<String, Boolean>) {
    val current = toggleState[label] ?: false
    SettingRow(label = label, value = if (current) "ON" else "OFF", onClick = { toggleState[label] = !current })
}

@Composable
private fun toggleRow(label: String, current: Boolean, toggleState: MutableMap<String, Boolean>, onChange: () -> Unit) {
    SettingRow(label = label, value = if (current) "ON" else "OFF", onClick = {
        toggleState[label] = !current
        onChange()
    })
}

@Composable
private fun choiceRow(label: String, value: String, options: List<String>, choiceState: MutableMap<String, String>) {
    SettingRow(label = label, value = value, onClick = {
        choiceState[label] = nextOption(value, options)
    })
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            Text(value, color = Color.White.copy(alpha = 0.72f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
        HorizontalDivider(color = Color(0xFF2A2A2A))
    }
}

private fun nextOption(current: String, options: List<String>): String {
    if (options.isEmpty()) return current
    val index = options.indexOf(current)
    return options[(if (index < 0) 0 else index + 1) % options.size]
}
