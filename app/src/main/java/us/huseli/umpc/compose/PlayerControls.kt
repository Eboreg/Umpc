package us.huseli.umpc.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.FastForward
import androidx.compose.material.icons.sharp.FastRewind
import androidx.compose.material.icons.sharp.HourglassBottom
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material.icons.sharp.Stop
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    playerState: PlayerState?,
    stopAfterCurrent: Boolean,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onStopLongClick: () -> Unit,
    onNextClick: () -> Unit,
    onReverseClick: () -> Unit,
    onForwardClick: () -> Unit,
) {
    val isStopped = playerState == null || playerState == PlayerState.STOP

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.height(80.dp).widthIn(max = 400.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.weight(0.6f).aspectRatio(1f, true),
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
                modifier = Modifier.weight(0.8f).aspectRatio(1f, true),
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
                modifier = Modifier.weight(1f).aspectRatio(1f, true),
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f, true)
                    .combinedClickable(
                        enabled = !isStopped,
                        onClick = onStopClick,
                        onLongClick = onStopLongClick,
                        indication = rememberRipple(bounded = false, radius = 20.dp),
                        interactionSource = remember { MutableInteractionSource() },
                    ),
            ) {
                val contentColor = LocalContentColor.current.copy(alpha = if (isStopped) 0.38f else 1f)

                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Sharp.Stop,
                        contentDescription =
                        if (stopAfterCurrent) stringResource(R.string.stop_after_current)
                        else stringResource(R.string.stop),
                    )
                    if (stopAfterCurrent) {
                        Icon(
                            modifier = Modifier.fillMaxSize(0.3f).align(Alignment.Center),
                            imageVector = Icons.Sharp.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background,
                        )
                    }
                }
            }

            IconButton(
                modifier = Modifier.weight(0.8f).aspectRatio(1f, true),
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
                modifier = Modifier.weight(0.6f).aspectRatio(1f, true),
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
}
