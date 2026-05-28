package com.calmlauncher.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ConfirmationSheet(message: String, confirmLabel: String = "Continue", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { CalmButton(text = confirmLabel, onClick = onConfirm) },
        dismissButton = { CalmButton(text = "Cancel", onClick = onDismiss) },
        title = { Text("Intentional action") },
        text = { Text(message) }
    )
}
