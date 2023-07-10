package us.huseli.umpc.compose.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FadingImageBox(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    verticalSpacing: Dp = 10.dp,
    colorStops: Array<Pair<Float, Color>>,
    image: @Composable BoxScope.() -> Unit,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val brush = Brush.verticalGradient(colorStops = colorStops)

    Box(modifier = modifier) {
        image()
        Box(modifier = Modifier.matchParentSize().background(brush = brush))
        topContent?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                content = it,
            )
        }
        bottomContent?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                content = it,
            )
        }
    }
}


@Composable
fun FadingImageBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    fadeStartY: Float = 0.75f,
    verticalSpacing: Dp = 10.dp,
    image: @Composable BoxScope.() -> Unit,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
    bottomContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    FadingImageBox(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalSpacing = verticalSpacing,
        colorStops = arrayOf(
            fadeStartY to Color.Transparent,
            1f to backgroundColor,
        ),
        image = image,
        topContent = topContent,
        bottomContent = bottomContent,
    )
}
