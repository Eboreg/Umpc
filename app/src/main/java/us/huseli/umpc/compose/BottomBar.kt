package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Headphones
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.compose.utils.AutoScrollingTextLine
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.CurrentSongViewModel

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    viewModel: CurrentSongViewModel = hiltViewModel(),
    onSurfaceClick: () -> Unit,
) {
    val albumArt by viewModel.currentSongAlbumArt.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val currentSongDuration by viewModel.currentSongDuration.collectAsStateWithLifecycle()
    val currentSongElapsed by viewModel.currentSongElapsed.collectAsStateWithLifecycle()
    val streamingUrl by viewModel.streamingUrl.collectAsStateWithLifecycle()
    val height = if (isInLandscapeMode()) 68.dp else 74.dp
    val streamingStarted =
        streamingUrl?.let { stringResource(R.string.streaming_from_x_started, it) }
        ?: stringResource(R.string.streaming_started)
    val streamingStopped = stringResource(R.string.streaming_stopped)

    val iconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = LocalContentColor.current.copy(0.5f)
    )

    BottomAppBar(
        modifier = modifier.clickable { onSurfaceClick() }.height(height),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (currentSongElapsed != null && currentSongDuration != null) {
                SongProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    elapsed = currentSongElapsed ?: 0.0,
                    duration = currentSongDuration ?: 0.0,
                    playerState = playerState,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(imageBitmap = albumArt?.thumbnail, modifier = Modifier.padding(end = 8.dp), forceSquare = true)

                Column(modifier = Modifier.weight(1f)) {
                    currentSong?.let {
                        AutoScrollingTextLine(it.title)
                        if (isInLandscapeMode()) {
                            Text("${it.artist} • ${it.album.name}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            AutoScrollingTextLine(it.artist, style = MaterialTheme.typography.bodySmall)
                            AutoScrollingTextLine(it.album.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (streamingUrl != null) {
                    IconToggleButton(
                        checked = isStreaming,
                        onCheckedChange = {
                            viewModel.toggleStream {
                                viewModel.addMessage(if (it) streamingStarted else streamingStopped)
                            }
                        },
                        colors = iconToggleButtonColors,
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Headphones,
                            contentDescription =
                            if (isStreaming) stringResource(R.string.stop_streaming)
                            else stringResource(R.string.start_streaming),
                            modifier = Modifier.fillMaxSize(0.8f),
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.playOrPause() },
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    if (playerState == PlayerState.PLAY) {
                        Icon(
                            imageVector = Icons.Sharp.Pause,
                            contentDescription = stringResource(R.string.pause),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Sharp.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
