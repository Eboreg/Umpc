package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import us.huseli.umpc.PlayerState
import us.huseli.umpc.formatDuration

@Composable
fun SongProgressSlider(
    modifier: Modifier = Modifier,
    elapsed: Double,
    duration: Double,
    playerState: PlayerState?,
    onManualChange: (Double) -> Unit,
) {
    var progress by remember(elapsed) { mutableStateOf(elapsed) }

    LaunchedEffect(playerState, elapsed) {
        while (playerState == PlayerState.PLAY) {
            delay(1000)
            progress++
        }
    }

    Column(modifier = modifier) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = progress.toFloat(),
            valueRange = 0f..duration.toFloat(),
            onValueChange = { progress = it.toDouble() },
            onValueChangeFinished = { onManualChange(progress) },
        )
        Row(
            modifier = modifier.fillMaxWidth().offset(y = (-8).dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = progress.formatDuration(),
                fontSize = 12.sp,
            )
            Text(
                text = duration.formatDuration(),
                fontSize = 12.sp,
                textAlign = TextAlign.End,
            )
        }
    }
}
