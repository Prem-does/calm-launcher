package com.calmlauncher.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.navigation.Routes

private val DockPanelHeight = 84.dp
private val DockShape = RoundedCornerShape(28.dp)
private val DockItemShape = RoundedCornerShape(18.dp)

@Composable
fun HomeDockNav(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.base),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(DockPanelHeight),
            shape = DockShape,
            color = CalmBlack.copy(alpha = 0.95f),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, CalmWhite.copy(alpha = 0.18f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.base, vertical = Spacing.base),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Routes.bottomTabs.forEach { route ->
                    val selected = route == current
                    val selectedSize by animateDpAsState(
                        targetValue = if (selected) 58.dp else 46.dp,
                        label = "dock-item-size-$route",
                    )
                    val labelAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.65f,
                        label = "dock-label-alpha-$route",
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.04f else 1f,
                        label = "dock-scale-$route",
                    )
                    val containerColor = if (selected) CalmWhite else Color.Transparent
                    val contentColor = if (selected) CalmBlack else CalmWhite
                    val interaction = remember(route) { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelect(route) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(selectedSize)
                                .clip(DockItemShape)
                                .background(containerColor)
                                .scale(scale),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = iconFor(route),
                                contentDescription = null,
                                tint = contentColor.copy(alpha = labelAlpha),
                            )
                        }

                        Text(
                            text = dockLabelFor(route).uppercase(),
                            style = CalmType.labelMd,
                            color = contentColor.copy(alpha = labelAlpha),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = Spacing.stackSm),
                        )
                    }
                }
            }
        }
    }
}

internal fun dockLabelFor(route: String): String = when (route) {
    Routes.HOME -> "Home"
    Routes.APPS -> "Apps"
    Routes.FOCUS -> "Focus"
    else -> route
}

private fun iconFor(route: String): ImageVector = when (route) {
    Routes.HOME -> Icons.Filled.Home
    Routes.APPS -> Icons.AutoMirrored.Filled.List
    Routes.FOCUS -> Icons.Filled.Timer
    else -> Icons.Filled.Home
}
