package com.calmlauncher.ui.screens

import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.calmlauncher.data.system.LauncherAppCatalog
import com.calmlauncher.data.system.LauncherAppItem
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.*
import com.calmlauncher.ui.components.BottomAction
import com.calmlauncher.ui.components.LauncherChrome
import com.calmlauncher.ui.components.ThinDivider
import kotlin.math.abs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    settings: LauncherSettingsState,
    recentApps: List<String>,
    calmInsight: String,
    dopamineRiskMessage: String,
    unlockCount: Int,
    onOpenApps: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (LauncherAppItem) -> Unit,
    onLongPressFocus: () -> Unit,
    onSetPreference: (String, String) -> Unit,
    screenTimeMinutes: Int,
    focusModeEnabled: Boolean
) {
    val context = LocalContext.current
    val apps = remember(settings, focusModeEnabled) {
        LauncherAppCatalog.loadAllowedApps(context, settings, focusModeEnabled)
    }
    val phoneApp = apps.firstOrNull { it.label.equals("Phone", ignoreCase = true) }
    val messagesApp = apps.firstOrNull { it.label.equals("Messages", ignoreCase = true) }
    val batteryPercent = remember(context) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 85
    }
    val now = LocalDateTime.now()
    val timeText = now.format(DateTimeFormatter.ofPattern("h:mm a"))
    val dateText = now.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    val useMinimalSurface = settings.shouldUseDynamicMinimalUi(dopamineRiskMessage)
    val contentAlignment = if (settings.centerAlignLayout() || useMinimalSurface) Alignment.CenterHorizontally else Alignment.Start
    val showNavigation = settings.showDisabledNavigation() && !useMinimalSurface
    val showSuggestions = settings.shouldShowAppSuggestions() && recentApps.isNotEmpty() && !useMinimalSurface
    val showRecent = settings.shouldShowRecentAppStrip() && recentApps.isNotEmpty() && !useMinimalSurface
    val showWidgets = !settings.disableWidgets() && !useMinimalSurface
    val clockStyle = if (settings.largeClockStyle() || settings.environmentModeChoiceValue() == "Drive") androidx.compose.material3.MaterialTheme.typography.displayLarge else androidx.compose.material3.MaterialTheme.typography.headlineLarge
    val infoStyle = if (settings.useComfortableDensity()) androidx.compose.material3.MaterialTheme.typography.bodyLarge else androidx.compose.material3.MaterialTheme.typography.labelSmall

    BackHandler(enabled = settings.shouldLockLauncherExit()) { }

    LauncherChrome(
        statusText = if (settings.showTime()) "$timeText • $dateText • ${batteryPercent}%" else dateText,
        rightActions = listOf("APPS", "SETTINGS"),
        onRightAction = { action ->
            when (action) {
                "APPS" -> onOpenApps()
                "SETTINGS" -> onOpenSettings()
            }
        },
        bottomActions = if (showNavigation) listOfNotNull(
            BottomAction("PHONE", onClick = { phoneApp?.let(onLaunchApp) }),
            BottomAction("MESSAGES", onClick = { messagesApp?.let(onLaunchApp) }),
            if (!settings.hideSearchBar()) BottomAction("SEARCH", onClick = onOpenSearch) else null,
            if (settings.swipeLeftForNotes()) BottomAction("NOTES", onClick = onOpenNotes) else null,
            BottomAction("HOME", selected = true, onClick = { })
        ) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .pointerInput(settings.preferences, settings.gestureNavigation()) {
                    detectTapGestures(onLongPress = { if (settings.requireLongPress() || settings.swipeRightForFocusMode()) onLongPressFocus() })
                }
                .pointerInput(settings.preferences, settings.gestureNavigation()) {
                    var dragX = 0f
                    var dragY = 0f
                    detectDragGestures(
                        onDragEnd = {
                            val horizontal = abs(dragX)
                            val vertical = abs(dragY)
                            if (!settings.gestureNavigation()) {
                                dragX = 0f
                                dragY = 0f
                                return@detectDragGestures
                            }
                            when {
                                vertical > horizontal && dragY > 110f && settings.swipeDownForSearch() -> onOpenSearch()
                                vertical > horizontal && dragY < -110f -> onOpenApps()
                                horizontal > vertical && dragX > 110f && settings.swipeRightForFocusMode() -> onOpenFocus()
                                horizontal > vertical && dragX < -110f && settings.swipeLeftForNotes() -> onOpenNotes()
                            }
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragCancel = {
                            dragX = 0f
                            dragY = 0f
                        }
                    ) { change, dragAmount ->
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                        change.consume()
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = contentAlignment
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (settings.showDate() || settings.showBattery() || settings.showWeather() || settings.showNextEvent()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (settings.showDate()) Text(dateText, color = Color.White.copy(alpha = 0.42f), style = infoStyle)
                        if (settings.showBattery()) Text("$batteryPercent%", color = Color.White.copy(alpha = 0.42f), style = infoStyle)
                        if (settings.showWeather()) Text("WEATHER READY", color = Color.White.copy(alpha = 0.42f), style = infoStyle)
                        if (settings.showNextEvent()) Text("NEXT EVENT CLEAR", color = Color.White.copy(alpha = 0.42f), style = infoStyle)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(text = "Focus on the essential.", style = clockStyle, color = Color.White)
                // Minimal 3x2 favorites grid like Olauncher: show icons (or labels) tappable
                val favPref = settings.preferences["Home Favorites"] ?: ""
                val favoriteItems: List<LauncherAppItem> = if (favPref.isNotBlank()) {
                    favPref.split(',').mapNotNull { id -> apps.firstOrNull { it.packageName == id.trim() || it.label.equals(id.trim(), ignoreCase = true) } }
                } else {
                    apps.take(6)
                }

                fun drawableToBitmap(drawable: Drawable): Bitmap {
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap?.let { return it }
                    }
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    return bmp
                }

                val showIcons = settings.showIconsOnHome()
                val compact = settings.compactHome()

                fun toggleFavorite(pkg: String) {
                    val cur = settings.preferences["Home Favorites"] ?: ""
                    val list = cur.split(',').map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                    if (list.contains(pkg)) {
                        list.remove(pkg)
                    } else {
                        if (list.size >= 6) {
                            list[list.size - 1] = pkg
                        } else {
                            list.add(pkg)
                        }
                    }
                    onSetPreference("Home Favorites", list.joinToString(","))
                }

                if (favoriteItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    // chunk into rows of 3
                    val rows = favoriteItems.chunked(3)
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            row.forEach { fav ->
                                val bitmap = remember(fav.packageName) {
                                    runCatching {
                                        val d = context.packageManager.getApplicationIcon(fav.packageName)
                                        drawableToBitmap(d)
                                    }.getOrNull()
                                }
                                val size = if (compact) 64.dp else 72.dp
                                if (showIcons && bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = fav.label,
                                        modifier = Modifier
                                            .size(size)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .pointerInput(fav.packageName) {
                                                detectTapGestures(onTap = { onLaunchApp(fav) }, onLongPress = { toggleFavorite(fav.packageName) })
                                            }
                                    )
                                } else {
                                    Text(
                                        fav.label.uppercase(),
                                        color = Color.White.copy(alpha = 0.95f),
                                        style = if (compact) androidx.compose.material3.MaterialTheme.typography.headlineMedium else androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                                        modifier = Modifier
                                            .clickable(onClick = { onLaunchApp(fav) })
                                            .pointerInput(fav.packageName) {
                                                detectTapGestures(onLongPress = { toggleFavorite(fav.packageName) }, onTap = { onLaunchApp(fav) })
                                            }
                                            .padding(vertical = if (compact) 6.dp else 12.dp, horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ThinDivider(modifier = Modifier.fillMaxWidth(0.14f))
                Spacer(modifier = Modifier.height(36.dp))
                if (settings.screenTimeCounter()) {
                    Text("SCREEN TIME ${screenTimeMinutes} MIN", color = Color.White.copy(alpha = 0.45f), style = infoStyle)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(if (focusModeEnabled) "FOCUS MODE ON" else "FOCUS MODE OFF", color = Color.White.copy(alpha = 0.45f), style = infoStyle)
                if (settings.environmentModeChoiceValue() != "Study") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(settings.environmentModeLabel().uppercase(), color = Color.White.copy(alpha = 0.45f), style = infoStyle)
                }
                if (settings.unlockCounter()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("UNLOCKS $unlockCount", color = Color.White.copy(alpha = 0.35f), style = infoStyle)
                }
                if (settings.dopamineDetectionEngine() && dopamineRiskMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(dopamineRiskMessage, color = Color.White.copy(alpha = 0.7f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Focus Mode is available.", color = Color.White.copy(alpha = 0.38f), style = infoStyle)
                }
                if (showSuggestions) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("SUGGESTIONS", color = Color.White.copy(alpha = 0.38f), style = infoStyle)
                    recentApps.take(3).forEach { app ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(app.uppercase(), color = Color.White.copy(alpha = 0.55f), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    }
                }
                if (showRecent) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("RECENT APPS", color = Color.White.copy(alpha = 0.38f), style = infoStyle)
                    recentApps.forEach { app ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(app.uppercase(), color = Color.White.copy(alpha = 0.45f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    }
                }
                if (calmInsight.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("CALM AI", color = Color.White.copy(alpha = 0.38f), style = infoStyle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(calmInsight, color = Color.White.copy(alpha = 0.7f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Did your phone help you today?", color = Color.White.copy(alpha = 0.38f), style = infoStyle)
                }
                if (showWidgets) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("CAMERA", color = Color.White.copy(alpha = 0.4f), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("BROWSER", color = Color.White.copy(alpha = 0.4f), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("CALENDAR", color = Color.White.copy(alpha = 0.4f), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("NOTES", color = Color.White.copy(alpha = 0.4f), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
