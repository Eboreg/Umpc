package us.huseli.umpc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun ListWithScrollbar(
    modifier: Modifier = Modifier,
    listSize: Int,
    listState: LazyListState,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    var scrollPos by rememberSaveable { mutableStateOf(0) }
    val handleWidthdp = 15.dp
    val handleHeightDp = 30.dp
    val handleHeightPx = with(LocalDensity.current) { handleHeightDp.toPx() }
    var maxHeightPx by remember { mutableStateOf(0f) }

    val draggableState = rememberDraggableState { delta ->
        if (scrollPos + delta >= 0 && scrollPos + delta + handleHeightPx <= maxHeightPx) {
            scrollPos += delta.toInt()
            scope.launch {
                val itemIndex = (listSize * (scrollPos / maxHeightPx)).toInt()
                if (itemIndex >= 0 && itemIndex < listSize)
                    listState.scrollToItem(itemIndex)
            }
        }
    }

    if (listSize > 0) {
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .map {
                    min(
                        ((it.toFloat() / listSize) * maxHeightPx).toInt(),
                        (maxHeightPx - handleHeightPx).toInt()
                    )
                }
                .distinctUntilChanged()
                .collect { scrollPos = it }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        content()
        Box(
            modifier = Modifier
                .offset { IntOffset(y = scrollPos, x = (maxWidth - handleWidthdp).toPx().toInt()) }
                .size(handleWidthdp, handleHeightDp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, shape = ShapeDefaults.ExtraSmall)
                .draggable(state = draggableState, orientation = Orientation.Vertical)
        )
    }
}
