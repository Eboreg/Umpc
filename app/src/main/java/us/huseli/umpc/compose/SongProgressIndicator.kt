package us.huseli.umpc.compose

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import us.huseli.umpc.PlayerState

fun getProgress(elapsed: Double, duration: Double) =
    duration.takeIf { it > 0 }?.let { (elapsed / it).toFloat() } ?: 0f

@Composable
fun SongProgressIndicator(
    modifier: Modifier = Modifier,
    elapsed: Double,
    duration: Double,
    enabled: Boolean,
    playerState: PlayerState?,
) {
    var mutableElapsed by remember(elapsed) { mutableDoubleStateOf(elapsed) }
    var progress by remember { mutableFloatStateOf(getProgress(elapsed, duration)) }

    LaunchedEffect(playerState, elapsed, duration, enabled) {
        while (playerState == PlayerState.PLAY && enabled) {
            progress = getProgress(mutableElapsed, duration)
            delay(500)
            mutableElapsed += 0.5
        }
    }

    LinearProgressIndicator(progress = progress, modifier = modifier)
}
