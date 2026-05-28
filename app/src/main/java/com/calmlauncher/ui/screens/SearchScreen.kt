package com.calmlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
fun SearchScreen(
    settings: LauncherSettingsState,
    recentApps: List<String>,
    onBack: () -> Unit,
    onLaunchApp: (LauncherAppItem) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps = remember(settings) { LauncherAppCatalog.loadSearchApps(context, settings) }
    val visibleApps = remember(query, apps) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    BackHandler(enabled = settings.shouldLockLauncherExit()) { }
    LauncherChrome(
        statusText = if (settings.showTime()) "SEARCH • TEXT ONLY" else "SEARCH",
        rightActions = listOf("MENU", "LOG"),
        bottomActions = null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("TYPE TO SEARCH") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF707070))
            Spacer(modifier = Modifier.height(24.dp))

            if (settings.shouldShowSuggestions() && recentApps.isNotEmpty()) {
                Text("RECENT", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                recentApps.take(4).forEach { item ->
                    Text(item, color = Color.White.copy(alpha = 0.75f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            visibleApps.groupBy { if (settings.verticalListNavigation()) "ALL" else (it.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#") }
                .toSortedMap()
                .forEach { (section, items) ->
                Text(section, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Text(
                        item.label,
                        color = Color.White,
                        style = if (settings.showSearchOnlyText()) androidx.compose.material3.MaterialTheme.typography.labelSmall else androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { onLaunchApp(item) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CalmButton(text = "SEARCH", onClick = onBack)
                CalmButton(text = "HOME", onClick = onBack)
            }
        }
    }
}
