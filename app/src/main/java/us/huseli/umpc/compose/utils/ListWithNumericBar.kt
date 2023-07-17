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
import androidx.compose.runtime.LaunchedEffect
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
import kotlin.math.roundToInt

@Composable
fun ListWithNumericBar(
    modifier: Modifier = Modifier,
    scope: CoroutineScope = rememberCoroutineScope(),
    listState: LazyListState,
    minItems: Int = 50,
    barWidth: Dp = 30.dp,
    listSize: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    var maxHeightDp by remember { mutableStateOf(0.dp) }
    var itemIndices by remember { mutableStateOf<List<Int>>(emptyList()) }

    LaunchedEffect(maxHeightDp, listSize) {
        val maxIndices = (maxHeightDp / 30.dp).toInt()
        val maxValue = listSize - 1
        val increment = maxValue.toFloat() / (maxIndices - 1)
        val tempIndices = mutableListOf<Int>()

        for (i in 0 until maxIndices) {
            val index = (i * increment).roundToInt()
            if (!tempIndices.contains(index)) tempIndices.add(index)
        }
        itemIndices = tempIndices
    }

    BoxWithConstraints(modifier = modifier) {
        maxHeightDp = maxHeight
        Row {
            Column(modifier = Modifier.weight(1f)) {
                content()
            }

            if (itemIndices.isNotEmpty() && listSize >= minItems) {
                Box(modifier = Modifier.width(barWidth).fillMaxHeight()) {
                    itemIndices.forEachIndexed { index, itemIndex ->
                        Box(
                            modifier = Modifier
                                .offset(0.dp, maxHeightDp * (index.toFloat() / itemIndices.size))
                                .size(width = barWidth, height = 30.dp)
                                .clickable { scope.launch { listState.scrollToItem(itemIndex) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = (itemIndex + 1).toString(),
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
