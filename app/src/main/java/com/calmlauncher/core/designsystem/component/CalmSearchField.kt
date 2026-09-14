package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/** One consistent search treatment used across every searchable launcher surface. */
@Composable
fun CalmSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search",
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current
    val textStyle = CalmType.bodyMd.copy(color = CalmWhite)
    val shape = RoundedCornerShape(14.dp)
    val clearInteraction = remember { MutableInteractionSource() }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester == null) Modifier
                else Modifier.focusRequester(focusRequester),
            ),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(CalmWhite),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .background(CalmSurfaceContainer, shape)
                    .padding(horizontal = Spacing.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = CalmGray,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(text = placeholder, style = textStyle.copy(color = CalmGray))
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        tint = CalmGray,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = clearInteraction,
                                indication = null,
                                role = Role.Button,
                                onClick = { onQueryChange("") },
                            ),
                    )
                }
            }
        },
    )
}