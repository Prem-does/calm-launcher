package com.calmlauncher.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import com.calmlauncher.core.designsystem.component.AppListRow
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * The Search screen: a single, autofocused query field styled with a bottom border only,
 * and a live list of matching apps beneath it. Results deliberately include hidden/social
 * apps so they stay reachable by explicit intent. Selecting a result opens it (through the
 * friction pipeline) and closes search; the [CalmBackBar] is the back affordance.
 */
@Composable
fun SearchScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    var isClosing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val requestClose: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            focusManager.clearFocus()
            onClose()
        }
    }

    BackHandler(enabled = true) { requestClose() }

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Search", onBack = requestClose) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -40f) requestClose()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 40f) requestClose()
                    }
                },
        ) {
            item {
                SearchField(
                    value = query,
                    onValueChange = viewModel::onQuery,
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Spacing.marginMobile,
                            vertical = Spacing.gutter,
                        ),
                )
            }
            items(results, key = { it.packageName }) { app ->
                AppListRow(
                    label = app.label,
                    onClick = {
                        if (!isClosing) {
                            viewModel.open(app)
                            requestClose()
                        }
                    },
                )
            }
        }
    }
}

/** Thickness of the field's underline. */
private val UnderlineThickness = 1.dp

/**
 * A bare single-line text field: white text, a 1dp [CalmWhite] bottom border and a
 * [CalmGray] "Search" placeholder. No Material container — just the underline, matching the
 * monochrome Stitch aesthetic.
 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier,
) {
    val textStyle: TextStyle = CalmType.bodyLg.copy(color = CalmWhite)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .drawBehind {
                val y = size.height - UnderlineThickness.toPx() / 2f
                drawLine(
                    color = CalmWhite,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = UnderlineThickness.toPx(),
                )
            }
            .padding(bottom = Spacing.base),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(CalmWhite),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(text = "Search", style = textStyle.copy(color = CalmGray))
                }
                innerTextField()
            }
        },
    )
}
