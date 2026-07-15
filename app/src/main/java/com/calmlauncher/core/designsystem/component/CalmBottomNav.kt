package com.calmlauncher.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility wrapper that preserves the older bottom-nav entrypoint while rendering
 * the new floating dock UI from [HomeDockNav].
 */
@Composable
fun CalmBottomNav(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeDockNav(
        current = current,
        onSelect = onSelect,
        modifier = modifier,
    )
}
