package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import us.huseli.umpc.PlayerState
import us.huseli.retaintheme.formatDuration

@Composable
fun SongProgressSlider(
    modifier: Modifier = Modifier,
    elapsed: Double,
    duration: Double,
    backgroundAlpha: Float = 0.5f,
    playerState: PlayerState?,
    shape: CornerBasedShape = MaterialTheme.shapes.extraLarge,
    enabled: Boolean,
    onManualChange: (Double) -> Unit,
) {
    var progress by remember(elapsed) { mutableDoubleStateOf(elapsed) }

    LaunchedEffect(playerState, elapsed, enabled) {
        while (playerState == PlayerState.PLAY && enabled) {
            delay(1000)
            progress++
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.height(IntrinsicSize.Min)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha),
            shape = shape,
            content = {}
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
            Text(
                text = progress.formatDuration(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier.width(48.dp),
            )
            Slider(
                modifier = Modifier.weight(1f),
                value = progress.toFloat(),
                valueRange = 0f..duration.toFloat(),
                enabled = enabled,
                onValueChange = { progress = it.toDouble() },
                onValueChangeFinished = { onManualChange(progress) },
            )
            Text(
                text = duration.formatDuration(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier.width(48.dp).padding(end = 10.dp),
            )
        }
    }
}
