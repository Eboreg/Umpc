package us.huseli.umpc.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import us.huseli.umpc.prune

@Composable
fun <T> ListWithAlphabetBar(
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
    characters: Collection<Char>,
    listState: LazyListState,
    minItems: Int = 50,
    width: Dp = 30.dp,
    items: List<T>,
    selector: (T) -> String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var maxHeightDp by remember { mutableStateOf(0.dp) }

    BoxWithConstraints(modifier = modifier) {
        maxHeightDp = maxHeight
        Row {
            Column(modifier = Modifier.weight(1f)) {
                content()
            }

            if (characters.isNotEmpty() && items.size >= minItems) {
                val maxCharacters = (maxHeightDp / 30.dp).toInt()
                val displayedCharacters = characters.prune(maxCharacters)

                Box(modifier = Modifier.width(width).fillMaxHeight()) {
                    displayedCharacters.forEachIndexed { index, char ->
                        Box(
                            modifier = Modifier
                                .offset(0.dp, maxHeightDp * (index.toFloat() / displayedCharacters.size))
                                .size(width = width, height = 30.dp)
                                .clickable {
                                    scope.launch {
                                        if (char == '#') listState.scrollToItem(0)
                                        else {
                                            items.indexOfFirst { selector(it).startsWith(char, true) }
                                                .takeIf { it > -1 }
                                                ?.also { pos -> listState.scrollToItem(pos) }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = char.toString(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}
