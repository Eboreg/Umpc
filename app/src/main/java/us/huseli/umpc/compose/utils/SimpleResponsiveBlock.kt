package us.huseli.umpc.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.umpc.isInLandscapeMode

@Composable
fun SimpleResponsiveBlock(
    modifier: Modifier = Modifier,
    verticalDistance: Dp = 0.dp,
    horizontalDistance: Dp = 0.dp,
    content1: @Composable ColumnScope.() -> Unit,
    content2: @Composable ColumnScope.() -> Unit,
) {
    if (isInLandscapeMode()) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(horizontalDistance)) {
            Column(modifier = Modifier.weight(0.5f), content = content1)
            Column(modifier = Modifier.weight(0.5f), content = content2)
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            content1()
            Spacer(modifier = Modifier.height(verticalDistance))
            content2()
        }
    }
}
