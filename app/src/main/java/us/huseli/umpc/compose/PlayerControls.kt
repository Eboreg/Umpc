package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.Repeat
import androidx.compose.material.icons.sharp.Shuffle
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material.icons.sharp.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerState: PlayerState?,
    repeatState: Boolean,
    randomState: Boolean,
    buttonHeight: Dp = 40.dp,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onRandomClick: () -> Unit,
) {
    val isStopped = playerState == null || playerState == PlayerState.STOP
    val buttonModifier = Modifier.aspectRatio(1f, true).fillMaxHeight()
    val iconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = LocalContentColor.current.copy(0.5f)
    )

    Row(modifier = modifier.height(buttonHeight), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconToggleButton(
            modifier = buttonModifier,
            checked = repeatState,
            onCheckedChange = { onRepeatClick() },
            colors = iconToggleButtonColors,
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(0.66f),
                imageVector = Icons.Sharp.Repeat,
                contentDescription = stringResource(R.string.repeat),
            )
        }

        IconButton(
            modifier = buttonModifier,
            onClick = onPreviousClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(0.75f),
                imageVector = Icons.Sharp.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
            )
        }

        IconButton(
            modifier = buttonModifier,
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
            modifier = buttonModifier,
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
            modifier = buttonModifier,
            onClick = onNextClick,
            enabled = !isStopped
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(0.75f),
                imageVector = Icons.Sharp.SkipNext,
                contentDescription = stringResource(R.string.next),
            )
        }

        IconToggleButton(
            modifier = buttonModifier,
            checked = randomState,
            onCheckedChange = { onRandomClick() },
            colors = iconToggleButtonColors,
        ) {
            Icon(
                modifier = Modifier.fillMaxSize(0.66f),
                imageVector = Icons.Sharp.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
            )
        }
    }
}
