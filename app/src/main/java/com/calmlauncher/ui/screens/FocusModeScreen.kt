package com.calmlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.*
import com.calmlauncher.ui.components.CalmButton
import com.calmlauncher.ui.components.LauncherChrome

@Composable
fun FocusModeScreen(
    settings: LauncherSettingsState,
    onExit: () -> Unit,
    focusModeEnabled: Boolean,
    onEmergencyBypass: () -> Unit
) {
    BackHandler(enabled = focusModeEnabled) { }
    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window

    DisposableEffect(focusModeEnabled) {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).hide(
                android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
            )
            window.setDecorFitsSystemWindows(false)
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(
                    android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
                )
                window.setDecorFitsSystemWindows(true)
            }
        }
    }

    LauncherChrome(
        statusText = if (settings.showTime()) "FOCUS • ${if (focusModeEnabled) "ON" else "OFF"}" else "FOCUS",
        rightActions = listOf("MENU", "LOG"),
        bottomActions = null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                if (!settings.blankScreenMode()) {
                    Text(
                        if (settings.calmQuotes()) "Be here now." else "Focus mode active.",
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        when {
                            settings.emergencyBypassEnabled() -> "Tap to exit. Long-press for emergency bypass."
                            settings.lockDeviceDuringFocusSessions() -> "Locked until session ends."
                            else -> "Minimal surface enabled."
                        },
                        color = Color.White.copy(alpha = 0.55f),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                    if (settings.focusTimer() || settings.pomodoroMode() || settings.meditationTimer() || settings.deepWorkSessionTimer()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text("SESSION TIMER READY", color = Color.White.copy(alpha = 0.45f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                    if (settings.focusSounds() || settings.meditationAmbientSounds() || settings.ambientRainSounds()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("SOUND LAYER ACTIVE", color = Color.White.copy(alpha = 0.4f), style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                }
            }
            CalmButton(
                text = if (focusModeEnabled) "EXIT FOCUS" else "ENTER FOCUS",
                onClick = {
                    if (focusModeEnabled && settings.shouldAllowEmergencyBypass()) {
                        onEmergencyBypass()
                    } else {
                        onExit()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
            )
        }
    }
}
