package com.calmlauncher.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.calmAIAssistant
import com.calmlauncher.launcher.usageReflectionScreen
import com.calmlauncher.ui.components.CalmButton

@Composable
fun ToolScreen(
    name: String,
    settings: LauncherSettingsState,
    screenTimeMinutes: Int,
    unlockCount: Int,
    calmInsight: String,
    dopamineRiskMessage: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(name.uppercase(), color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(18.dp))
        if (settings.usageReflectionScreen()) {
            Text("SCREEN TIME $screenTimeMinutes MIN", color = Color.White.copy(alpha = 0.72f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("UNLOCKS $unlockCount", color = Color.White.copy(alpha = 0.72f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Did your phone help you today?", color = Color.White.copy(alpha = 0.72f))
            Spacer(modifier = Modifier.height(18.dp))
        }
        if (settings.calmAIAssistant()) {
            Text(if (dopamineRiskMessage.isNotBlank()) dopamineRiskMessage else calmInsight, color = Color.White.copy(alpha = 0.72f))
            Spacer(modifier = Modifier.height(18.dp))
            Text("Take a 5 minute break.", color = Color.White.copy(alpha = 0.5f))
        } else {
            Text("Minimal tool surface", color = Color.White.copy(alpha = 0.72f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        CalmButton(text = "BACK", onClick = onBack)
    }
}
