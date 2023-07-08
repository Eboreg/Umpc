package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode

@Composable
fun SmallSongRowContent(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    showEnqueueButton: Boolean = true,
) {
    val durationAndYear = listOfNotNull(song.duration?.formatDuration(), song.year?.toString())
    val titleRow =
        if (song.discNumber != null && song.trackNumber != null) "${song.discNumber}-${song.trackNumber}. ${song.title}"
        else if (song.trackNumber != null) "${song.trackNumber}. ${song.title}"
        else song.title

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 10.dp),
    ) {
        Text(
            text = titleRow,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isInLandscapeMode()) {
            Text(text = durationAndYear.joinToString(" • "), fontSize = 12.sp)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                durationAndYear.forEach {
                    Text(text = it, fontSize = 12.sp, textAlign = TextAlign.End)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.End) {
            if (showEnqueueButton) {
                IconButton(onClick = onEnqueueClick) {
                    Icon(Icons.Sharp.QueueMusic, stringResource(R.string.enqueue))
                }
            }
            IconButton(onClick = onPlayPauseClick) {
                if (isCurrentSong && playerState == PlayerState.PLAY)
                    Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
            }
        }
    }
}

@Composable
fun SmallSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
    showEnqueueButton: Boolean = true,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val tonalElevation = LocalAbsoluteTonalElevation.current + if (isCurrentSong) 5.dp else 0.dp

    CompositionLocalProvider(LocalAbsoluteTonalElevation provides tonalElevation) {
        Surface {
            if (isExpanded) {
                ExpandedSongRow(
                    modifier = modifier,
                    song = song,
                    isCurrentSong = isCurrentSong,
                    playerState = playerState,
                    position = song.trackNumber,
                    discNumber = song.discNumber,
                    showAlbumArt = false,
                    onClick = { isExpanded = false },
                    onPlayPauseClick = onPlayPauseClick,
                    onEnqueueClick = onEnqueueClick,
                    onGotoArtistClick = onGotoArtistClick,
                    onGotoAlbumClick = onGotoAlbumClick,
                )
            } else {
                SmallSongRowContent(
                    modifier = modifier.clickable { isExpanded = !isExpanded },
                    song = song,
                    isCurrentSong = isCurrentSong,
                    playerState = playerState,
                    onPlayPauseClick = onPlayPauseClick,
                    onEnqueueClick = onEnqueueClick,
                    showEnqueueButton = showEnqueueButton,
                )
            }
        }
    }
}
