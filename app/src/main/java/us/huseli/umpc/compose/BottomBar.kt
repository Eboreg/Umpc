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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val albumArt by viewModel.currentSongAlbumArt.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val currentSongDuration by viewModel.currentSongDuration.collectAsStateWithLifecycle()
    val currentSongElapsed by viewModel.currentSongElapsed.collectAsStateWithLifecycle()
    val streamingUrl by viewModel.streamingUrl.collectAsStateWithLifecycle(null)
    val height = if (isInLandscapeMode()) 68.dp else 74.dp

    val iconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = LocalContentColor.current.copy(0.5f)
    )

    currentSong?.let { song ->
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
                        enabled = isConnected,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(
                        imageBitmap = albumArt?.thumbnail,
                        modifier = Modifier.padding(end = 8.dp),
                        forceSquare = true
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        AutoScrollingTextLine(song.title)
                        if (isInLandscapeMode()) {
                            Text("${song.artist} • ${song.album.name}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            AutoScrollingTextLine(song.artist, style = MaterialTheme.typography.bodySmall)
                            AutoScrollingTextLine(song.album.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (streamingUrl != null) {
                        IconToggleButton(
                            checked = isStreaming,
                            enabled = isConnected,
                            onCheckedChange = {
                                viewModel.toggleStream { started ->
                                    viewModel.addMessage(
                                        if (started) {
                                            streamingUrl?.let {
                                                context.getString(R.string.streaming_from_x_started, it)
                                            }
                                            ?: context.getString(R.string.streaming_started)
                                        } else context.getString(R.string.streaming_stopped)
                                    )
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
                        modifier = Modifier.padding(end = 10.dp),
                        enabled = isConnected,
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
}
