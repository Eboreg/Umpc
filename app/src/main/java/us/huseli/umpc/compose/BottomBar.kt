package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbumArt

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    albumArt: MPDAlbumArt?,
    title: String?,
    artist: String?,
    album: String?,
    playerState: PlayerState?,
    isStreaming: Boolean,
    showStreamingIcon: Boolean,
    currentSongElapsed: Double?,
    currentSongDuration: Double?,
    onPlayPauseClick: () -> Unit,
    onSurfaceClick: () -> Unit,
    onStreamingChange: (Boolean) -> Unit,
) {
    val iconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = LocalContentColor.current.copy(0.5f)
    )

    BottomAppBar(
        modifier = modifier.clickable { onSurfaceClick() },
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (currentSongElapsed != null && currentSongDuration != null) {
                SongProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    elapsed = currentSongElapsed,
                    duration = currentSongDuration,
                    playerState = playerState,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(
                    imageBitmap = albumArt?.thumbnail,
                    modifier = Modifier.padding(end = 8.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    if (title != null) AutoScrollingTextLine(title)
                    if (artist != null) AutoScrollingTextLine(artist, fontSize = 12.sp)
                    if (album != null) AutoScrollingTextLine(album, fontSize = 12.sp)
                }

                if (showStreamingIcon) {
                    IconToggleButton(
                        checked = isStreaming,
                        onCheckedChange = onStreamingChange,
                        colors = iconToggleButtonColors,
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Headphones,
                            contentDescription =
                            if (isStreaming) stringResource(R.string.stop_streaming)
                            else stringResource(R.string.start_streaming),
                        )
                    }
                }

                IconButton(
                    onClick = onPlayPauseClick,
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
