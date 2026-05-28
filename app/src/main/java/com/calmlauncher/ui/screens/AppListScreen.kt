package com.calmlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calmlauncher.data.system.LauncherAppCatalog
import com.calmlauncher.data.system.LauncherAppItem
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.*
import com.calmlauncher.ui.components.CalmButton
import com.calmlauncher.ui.components.LauncherChrome

@Composable
fun AppListScreen(
    settings: LauncherSettingsState,
    focusModeEnabled: Boolean,
    onBack: () -> Unit,
    onLaunchApp: (LauncherAppItem) -> Unit
) {
    val context = LocalContext.current
    val apps = remember(settings, focusModeEnabled) { LauncherAppCatalog.loadAllowedApps(context, settings, focusModeEnabled) }
    BackHandler(enabled = settings.shouldLockLauncherExit()) { }
    LauncherChrome(
        statusText = if (settings.showTime()) "APPS • FILTERED" else "APPS",
        rightActions = listOf("MENU", "LOG"),
        bottomActions = null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp)) {
            if (settings.shouldShowRecentAppStrip()) {
                Text("RECENT IS ACTIVE", color = Color.White.copy(alpha = 0.45f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(12.dp))
            }
            apps.forEach { app ->
                Text(
                    text = app.label,
                    style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    modifier = Modifier.clickable { onLaunchApp(app) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2A2A2A))
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            CalmButton(text = "BACK", onClick = onBack)
        }
    }
}
