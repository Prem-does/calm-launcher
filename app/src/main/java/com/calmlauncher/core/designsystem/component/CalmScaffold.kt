package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmWhite

/**
 * The launcher's root layout. A pure-black [Scaffold] that hosts an optional [topBar]
 * and [bottomBar] and hands the resulting inset [PaddingValues] to [content]. We rely
 * on Material3's Scaffold purely for slot placement / inset accounting; the monochrome
 * surface is enforced via [CalmBlack] container colors so no tonal elevation leaks in.
 */
@Composable
fun CalmScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = CalmBlack,
        contentColor = CalmWhite,
        content = content,
    )
}
