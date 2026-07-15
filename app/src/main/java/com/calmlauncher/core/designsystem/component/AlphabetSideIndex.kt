package com.calmlauncher.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Stable
data class AlphabetIndex(
    val letters: List<Char>,
    val firstItemIndexByLetter: Map<Char, Int>,
)

@Composable
fun <T> rememberAlphabetIndex(
    items: List<T>,
    labelFor: (T) -> String,
): AlphabetIndex = remember(items) {
    val letters = ArrayList<Char>()
    val firstIndexByLetter = LinkedHashMap<Char, Int>()

    items.forEachIndexed { index, item ->
        val letter = labelFor(item).firstAlphabetLetterOrNull() ?: return@forEachIndexed
        if (letter !in firstIndexByLetter) {
            letters += letter
            firstIndexByLetter[letter] = index
        }
    }

    AlphabetIndex(
        letters = letters.distinct().sorted(),
        firstItemIndexByLetter = firstIndexByLetter,
    )
}

@Composable
fun AlphabetSideIndex(
    index: AlphabetIndex,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = Spacing.gutter),
) {
    if (index.letters.isEmpty()) return

    val scope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var isTouching by remember { mutableStateOf(false) }

    LaunchedEffect(isTouching, activeLetter) {
        if (!isTouching && activeLetter != null) {
            delay(420)
            activeLetter = null
        }
    }

    Box(modifier = modifier) {
        LetterRail(
            letters = index.letters,
            activeLetter = activeLetter,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(contentPadding)
                .pointerInput(index) {
                    fun selectAt(position: Offset, size: IntSize) {
                        val letter = letterAtPosition(
                            y = position.y,
                            height = size.height,
                            letters = index.letters,
                        ) ?: return
                        if (letter == activeLetter) return

                        activeLetter = letter
                        index.firstItemIndexByLetter[letter]?.let { itemIndex ->
                            scope.launch { listState.scrollToItem(itemIndex) }
                        }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isTouching = true
                        selectAt(down.position, size)

                        drag(down.id) { change ->
                            if (change.positionChange() != Offset.Zero) {
                                selectAt(change.position, size)
                                change.consume()
                            }
                        }

                        isTouching = false
                    }
                },
        )

        ActiveLetterOverlay(
            letter = activeLetter,
            visible = activeLetter != null,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun LetterRail(
    letters: List<Char>,
    activeLetter: Char?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = Spacing.base),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val selected = letter == activeLetter
            Text(
                text = letter.toString(),
                style = CalmType.labelMd.copy(
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
                ),
                color = if (selected) CalmWhite.copy(alpha = 0.78f) else CalmGrayDim.copy(alpha = 0.64f),
                textAlign = TextAlign.Center,
                modifier = Modifier.size(width = 28.dp, height = 18.dp),
            )
        }
    }
}

@Composable
private fun ActiveLetterOverlay(
    letter: Char?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = CalmSurfaceContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter?.toString().orEmpty(),
                style = CalmType.headlineLg.copy(fontWeight = FontWeight.Light),
                color = if (CalmBlack == CalmWhite) CalmGray else CalmWhite.copy(alpha = 0.82f),
            )
        }
    }
}

private fun letterAtPosition(
    y: Float,
    height: Int,
    letters: List<Char>,
): Char? {
    if (height <= 0 || letters.isEmpty()) return null
    val index = ((y.coerceIn(0f, height.toFloat() - 1f) / height.toFloat()) * letters.size)
        .toInt()
        .coerceIn(0, letters.lastIndex)
    return letters[index]
}

private fun String.firstAlphabetLetterOrNull(): Char? =
    firstOrNull { it.isLetter() }?.uppercaseChar()
