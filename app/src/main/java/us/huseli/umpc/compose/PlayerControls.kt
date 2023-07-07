package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FastForward
import androidx.compose.material.icons.sharp.FastRewind
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material.icons.sharp.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerState: PlayerState?,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onNextClick: () -> Unit,
    onReverseClick: () -> Unit,
    onForwardClick: () -> Unit,
) {
    val isStopped = playerState == null || playerState == PlayerState.STOP
    val buttonModifier = Modifier.aspectRatio(1f, true).fillMaxHeight()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = buttonModifier.weight(0.6f),
            onClick = onPreviousClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Sharp.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
            )
        }

        IconButton(
            modifier = buttonModifier.weight(0.75f),
            onClick = onReverseClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Sharp.FastRewind,
                contentDescription = stringResource(R.string.rewind),
            )
        }

        IconButton(
            modifier = buttonModifier.weight(1f),
            onClick = onPlayPauseClick
        ) {
            if (playerState == PlayerState.PLAY) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Sharp.Pause,
                    contentDescription = stringResource(R.string.pause),
                )
            } else {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Sharp.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                )
            }
        }

        IconButton(
            modifier = buttonModifier.weight(1f),
            onClick = onStopClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Sharp.Stop,
                contentDescription = stringResource(R.string.stop),
            )
        }

        IconButton(
            modifier = buttonModifier.weight(0.75f),
            onClick = onForwardClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Sharp.FastForward,
                contentDescription = stringResource(R.string.forward),
            )
        }

        IconButton(
            modifier = buttonModifier.weight(0.6f),
            onClick = onNextClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Sharp.SkipNext,
                contentDescription = stringResource(R.string.next),
            )
        }
    }
}
