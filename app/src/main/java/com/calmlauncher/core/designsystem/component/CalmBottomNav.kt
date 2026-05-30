package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.navigation.Routes

/** Opacity applied to the content of an unselected tab. */
private const val InactiveTabAlpha = 0.6f

/** Icon for a given bottom-nav route. */
private fun iconFor(route: String): ImageVector = when (route) {
    Routes.HOME -> Icons.Filled.Home
    Routes.APPS -> Icons.AutoMirrored.Filled.List
    Routes.FOCUS -> Icons.Filled.Timer
    else -> Icons.Filled.Home
}

/**
 * The three-tab bottom navigation (HOME / APPS / FOCUS from [Routes.bottomTabs]).
 * Each tab stacks its icon over an UPPERCASE [CalmType.labelLg] label. The [current]
 * tab is rendered as the signature inverted block — solid [CalmWhite] fill with
 * [CalmBlack] content — while the rest are white content dimmed to 60%. The bar is
 * [Spacing.bottomNavHeight] tall, opens with a [ThinDivider] and honours the nav-bar
 * inset. Ripple is suppressed to preserve the flat e-ink aesthetic.
 */
@Composable
fun CalmBottomNav(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(CalmBlack)) {
        ThinDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(Spacing.bottomNavHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Routes.bottomTabs.forEach { route ->
                NavTab(
                    route = route,
                    selected = route == current,
                    onClick = { onSelect(route) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun NavTab(
    route: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val contentColor: Color = if (selected) CalmBlack else CalmWhite
    val containerColor: Color = if (selected) CalmWhite else Color.Transparent
    val alpha = if (selected) 1f else InactiveTabAlpha

    Column(
        modifier = modifier
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.base, vertical = Spacing.base),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = iconFor(route),
            contentDescription = null,
            tint = contentColor.copy(alpha = alpha),
            modifier = Modifier.padding(bottom = Spacing.stackSm),
        )
        Text(
            text = route.uppercase(),
            style = CalmType.labelLg,
            color = contentColor.copy(alpha = alpha),
            textAlign = TextAlign.Center,
        )
    }
}
