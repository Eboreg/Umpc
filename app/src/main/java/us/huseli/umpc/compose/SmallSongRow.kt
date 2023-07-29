package us.huseli.umpc.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.data.MPDSong

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmallSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    discNumber: Int? = null,
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
                    discNumber = discNumber,
                    showAlbumArt = false,
                    onPlayPauseClick = onPlayPauseClick,
                    onEnqueueClick = onEnqueueClick,
                    onAddToPlaylistClick = onAddToPlaylistClick,
                    onGotoArtistClick = onGotoArtistClick,
                )
            } else {
                SmallSongRowContent(
                    song = song,
                    discNumber = discNumber,
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
