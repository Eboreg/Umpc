package us.huseli.umpc.compose.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AutoScrollingTextLine(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    pixelsPerSecond: Int = 100,
    delayMillis: Int = 2000,
) {
    val offset by remember { mutableStateOf(Animatable(0f)) }
    var containerWidth by remember { mutableStateOf(0f) }
    var textWidth by remember { mutableStateOf(0f) }
    val target = containerWidth - textWidth
    val durationMillis = (1000 * (abs(target) / pixelsPerSecond)).toInt() + (2 * delayMillis)

    LaunchedEffect(containerWidth, textWidth) {
        if (textWidth > containerWidth) {
            offset.animateTo(
                targetValue = containerWidth - textWidth,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        this.durationMillis = durationMillis
                        0f at 0
                        0f at delayMillis
                        target at durationMillis - delayMillis with LinearEasing
                        target at durationMillis
                    },
                )
            )
        } else {
            offset.snapTo(0f)
        }
    }

    Text(
        text = text,
        style = style,
        color = color,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Visible,
        softWrap = false,
        modifier = modifier
            .onMeasured { maxSize, measuredSize ->
                textWidth = measuredSize.width
                containerWidth = maxSize.width
            }
            .clipToBounds()
            .absoluteOffset { IntOffset(offset.value.roundToInt(), 0) }
    )
}

fun Modifier.onMeasured(callback: (Size, Size) -> Unit) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val maxIntrinsicSize = Size(
        measurable.maxIntrinsicWidth(constraints.maxHeight).toFloat(),
        measurable.maxIntrinsicHeight(constraints.maxWidth).toFloat()
    )
    val measuredSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())

    callback(measuredSize, maxIntrinsicSize)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

@Composable
fun AutoScrollingTextLine(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    pixelsPerSecond: Int = 100,
    delayMillis: Int = 2000,
) {
    AutoScrollingTextLine(
        AnnotatedString(text),
        modifier,
        style,
        color,
        fontSize,
        pixelsPerSecond,
        delayMillis
    )
}
