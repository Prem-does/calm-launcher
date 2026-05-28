package com.calmlauncher.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmlauncher.launcher.shouldQuietInteractions
import com.calmlauncher.ui.theme.LocalLauncherSettings

@Composable
fun CalmButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val settings = LocalLauncherSettings.current
    val haptics = LocalHapticFeedback.current
    val clickHandler = remember(settings.shouldQuietInteractions(), onClick) {
        {
            if (!settings.shouldQuietInteractions()) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        }
    }
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
            .clickable(onClick = clickHandler)
            .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp)),
        color = MaterialTheme.colorScheme.onBackground
    )
}
