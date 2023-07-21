package us.huseli.umpc.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmallSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    isExpanded: Boolean,
    isSelected: Boolean,
    playerState: PlayerState?,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showArtist: Boolean = false,
    showYear: Boolean = true,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onGotoArtistClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val tonalElevation = LocalAbsoluteTonalElevation.current + if (isCurrentSong) 5.dp else 0.dp
    var surfaceModifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    if (isSelected) surfaceModifier = surfaceModifier.border(width = 3.dp, color = MaterialTheme.colorScheme.primary)

    CompositionLocalProvider(LocalAbsoluteTonalElevation provides tonalElevation) {
        Surface(modifier = surfaceModifier, contentColor = color) {
            if (isExpanded) {
                ExpandedSongRow(
                    modifier = modifier,
                    song = song,
                    isCurrentSong = isCurrentSong,
                    playerState = playerState,
                    position = song.trackNumber,
                    discNumber = song.discNumber,
                    showAlbumArt = false,
                    onPlayPauseClick = onPlayPauseClick,
                    onEnqueueClick = onEnqueueClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            } else {
                SmallSongRowContent(
                    song = song,
                    isCurrentSong = isCurrentSong,
                    showArtist = showArtist || song.artist != song.album.artist,
                    showYear = showYear,
                    playerState = playerState,
                    onPlayPauseClick = onPlayPauseClick,
                )
            }
        }
    }
}

@Composable
fun SmallSongRowContent(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    showArtist: Boolean = false,
    showYear: Boolean = true,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
) {
    val durationAndYear = listOfNotNull(
        song.duration?.formatDuration(),
        if (showYear) song.year?.toString() else null,
    )
    val numbers =
        if (song.discNumber != null && song.trackNumber != null) "${song.discNumber}-${song.trackNumber}"
        else if (song.trackNumber != null) song.trackNumber.toString()
        else null

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 10.dp),
    ) {
        numbers?.let {
            Text(
                text = it,
                modifier = Modifier.width(30.dp).padding(end = 5.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showArtist) {
                Text(
                    text = song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (isInLandscapeMode()) {
            Text(text = durationAndYear.joinToString(" • "), fontSize = 12.sp)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                durationAndYear.forEach {
                    Text(text = it, fontSize = 12.sp, textAlign = TextAlign.End)
                }
            }
        }
        IconButton(onClick = onPlayPauseClick) {
            if (isCurrentSong && playerState == PlayerState.PLAY)
                Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
            else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
        }
    }
}
