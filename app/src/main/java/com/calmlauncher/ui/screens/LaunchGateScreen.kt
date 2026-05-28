package com.calmlauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.calmlauncher.data.system.LauncherAppCatalog
import com.calmlauncher.domain.models.AppLaunchRequest
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.*
import com.calmlauncher.ui.components.CalmButton
import com.calmlauncher.ui.components.LauncherChrome

@Composable
fun LaunchGateScreen(
    request: AppLaunchRequest,
    settings: LauncherSettingsState,
    onRecordLaunchReason: (String, String, String) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val secondsRemaining = remember(request.packageName) { mutableIntStateOf(request.delaySeconds) }
    val confirmed = remember(request.packageName) { mutableStateOf(!request.requiresConfirmation) }
    val reasonRequired = settings.intentBasedAppOpening() || settings.frictionLevelChoiceValue() != "Light" || settings.analogMode() || settings.breathUnlock()
    val breathRequired = settings.breathUnlock() || settings.frictionLevelChoiceValue() == "Hardcore Monk"
    val breathSecondsRemaining = remember(request.packageName) { mutableIntStateOf(if (breathRequired) 12 else 0) }
    val reasonInput = remember(request.packageName) { mutableStateOf("") }
    val reasonShown = remember(request.packageName) { mutableStateOf(false) }
    val pinRequired = settings.requirePinForApps() && settings.pinProtected && !request.blocked
    val pinUnlocked = remember(request.packageName) { mutableStateOf(!pinRequired) }
    val pinInput = remember(request.packageName) { mutableStateOf("") }
    val pinFailure = remember(request.packageName) { mutableStateOf(false) }

    if (request.deadEndFeed) {
        DeadEndFeedScreen(onDone = onDone)
        return
    }

    if (pinRequired && !pinUnlocked.value) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                CalmButton(text = "UNLOCK", onClick = {
                    if (settings.pinHash == pinInput.value.sha256()) {
                        pinUnlocked.value = true
                        pinFailure.value = false
                    } else {
                        pinFailure.value = true
                    }
                })
            },
            dismissButton = { CalmButton(text = "BACK", onClick = onDone) },
            title = { Text("PIN REQUIRED") },
            text = {
                Column {
                    Text("Enter your launcher PIN to continue.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = pinInput.value, onValueChange = { pinInput.value = it }, label = { Text("PIN") })
                    if (pinFailure.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Incorrect PIN.", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        )
        return
    }

    if (request.blocked) {
        LauncherChrome(
            statusText = if (settings.showTime()) "BLOCKED • ${request.label.uppercase()}" else "BLOCKED",
            rightActions = listOf("MENU", "LOG"),
            bottomActions = null
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(request.label, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth(0.42f))
                Spacer(modifier = Modifier.height(18.dp))
                Text("This app is blocked by your launcher rules.", color = Color.White.copy(alpha = 0.72f))
                Spacer(modifier = Modifier.height(22.dp))
                CalmButton(text = "BACK", onClick = onDone)
            }
        }
        return
    }

    fun launchApp() {
        if (reasonRequired && reasonInput.value.isBlank()) {
            reasonShown.value = true
            return
        }
        if (breathSecondsRemaining.intValue > 0) {
            return
        }
        onRecordLaunchReason(request.packageName, request.label, reasonInput.value.ifBlank { "intent not entered" })
        LauncherAppCatalog.launchApp(context, request.packageName)
        onDone()
    }

    LaunchedEffect(request.packageName, pinUnlocked.value) {
        if (!pinUnlocked.value) return@LaunchedEffect
        while (secondsRemaining.intValue > 0) {
            kotlinx.coroutines.delay(1000)
            secondsRemaining.intValue -= 1
        }
        while (breathSecondsRemaining.intValue > 0) {
            kotlinx.coroutines.delay(1000)
            breathSecondsRemaining.intValue -= 1
        }
        if (confirmed.value && (!reasonRequired || reasonInput.value.isNotBlank())) {
            launchApp()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(request.label, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth(0.42f))
        Spacer(modifier = Modifier.height(24.dp))

        if (request.delaySeconds > 0 && secondsRemaining.intValue > 0) {
            Text("Opening in ${secondsRemaining.intValue}", color = Color.White.copy(alpha = 0.75f))
        }

        if (request.requiresConfirmation && !confirmed.value) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("This app may pull attention away from what matters.", color = Color.White.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(18.dp))
            CalmButton(text = "ARE YOU SURE?", onClick = {
                confirmed.value = true
                if (secondsRemaining.intValue == 0 && (!reasonRequired || reasonInput.value.isNotBlank())) {
                    launchApp()
                }
            })
        }

        if (reasonRequired) {
            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth(0.42f))
            Spacer(modifier = Modifier.height(18.dp))
            Text("Why are you opening this?", color = Color.White.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = reasonInput.value,
                onValueChange = {
                    reasonInput.value = it
                    reasonShown.value = false
                },
                label = { Text("Reason") },
                modifier = Modifier.fillMaxWidth(0.88f)
            )
            if (reasonShown.value && reasonInput.value.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("A short reason keeps the launch intentional.", color = Color.White.copy(alpha = 0.62f))
            }
        }

        if (breathRequired) {
            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.fillMaxWidth(0.42f))
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                if (breathSecondsRemaining.intValue > 0) "Breathe before opening: ${breathSecondsRemaining.intValue}s" else "Breathing pause complete.",
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when (breathSecondsRemaining.intValue % 3) {
                    0 -> "Inhale."
                    1 -> "Hold."
                    else -> "Exhale."
                },
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))
        CalmButton(
            text = when {
                breathSecondsRemaining.intValue > 0 -> "BREATHE"
                reasonRequired && reasonInput.value.isBlank() -> "ADD REASON"
                settings.oneAppAtATimeMode() -> "OPEN AND REPLACE"
                else -> "OPEN APP"
            },
            onClick = {
                if (confirmed.value && secondsRemaining.intValue == 0) {
                    launchApp()
                } else if (!request.requiresConfirmation) {
                    confirmed.value = true
                }
            }
        )
        if (settings.oneAppAtATimeMode()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("One-App-At-A-Time mode keeps the launcher path single-tasked.", color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun DeadEndFeedScreen(onDone: () -> Unit) {
    val breathingSeconds = remember { mutableIntStateOf(24) }
    LaunchedEffect(Unit) {
        while (breathingSeconds.intValue > 0) {
            kotlinx.coroutines.delay(1000)
            breathingSeconds.intValue -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Go outside.", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.22f), modifier = Modifier.fillMaxWidth(0.42f))
        Spacer(modifier = Modifier.height(18.dp))
        Text("Quote: the loop breaks when you stop feeding it.", color = Color.White.copy(alpha = 0.78f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Breathing reset: ${breathingSeconds.intValue}s", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            when (breathingSeconds.intValue % 3) {
                0 -> "Inhale."
                1 -> "Hold."
                else -> "Exhale."
            },
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Journal prompt: What are you avoiding right now?", color = Color.White.copy(alpha = 0.72f))
        Spacer(modifier = Modifier.height(22.dp))
        CalmButton(text = "BACK", onClick = onDone)
    }
}

private fun String.sha256(): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(toByteArray()).joinToString("") { "%02x".format(it) }
}
