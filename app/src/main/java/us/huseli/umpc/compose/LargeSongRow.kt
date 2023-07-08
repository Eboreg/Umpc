package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ImageBitmap
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
import us.huseli.umpc.mpd.highlight

@Composable
fun SongRowArtistAlbumInfo(artist: String?, album: String?, highlight: String?) {
    if (artist != null || album != null) {
        val fontSize = 12.sp
        val color = MaterialTheme.colorScheme.onSurfaceVariant

        if (isInLandscapeMode()) {
            Text(
                text = listOfNotNull(artist, album).joinToString(" • ").highlight(highlight),
                fontSize = fontSize,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            if (artist != null) {
                Text(
                    text = artist.highlight(highlight),
                    fontSize = fontSize,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (album != null) {
                Text(
                    text = album.highlight(highlight),
                    fontSize = fontSize,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun LargeSongRowContent(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    albumArt: ImageBitmap?,
    albumArtModifier: Modifier = Modifier,
    position: Int? = null,
    discNumber: Int? = null,
    artist: String? = null,
    album: String? = null,
    showEnqueueButton: Boolean = true,
    highlight: String? = null,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
) {
    val titleRow =
        if (discNumber != null && position != null) "${discNumber}-${position}. ${song.title}"
        else if (position != null) "${position}. ${song.title}"
        else song.title

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            imageBitmap = albumArt,
            altIcon = Icons.Sharp.MusicNote,
            forceSquare = true,
            modifier = albumArtModifier
        )
        Column(modifier = Modifier.padding(start = 8.dp).padding(vertical = 8.dp).weight(1f)) {
            Text(
                text = titleRow.highlight(highlight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    SongRowArtistAlbumInfo(artist, album, highlight)
                }
                if (song.duration != null || song.year != null) {
                    if (isInLandscapeMode() || artist == null || album == null) {
                        Text(
                            text = listOfNotNull(
                                song.duration?.formatDuration(),
                                song.year?.toString()
                            ).joinToString(" • "),
                            fontSize = 12.sp,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            song.duration?.formatDuration()?.let {
                                Text(text = it, fontSize = 12.sp, textAlign = TextAlign.End)
                            }
                            song.year?.let {
                                Text(text = it.toString(), fontSize = 12.sp, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.width(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
fun LargeSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    albumArt: ImageBitmap?,
    albumArtModifier: Modifier = Modifier,
    position: Int? = null,
    discNumber: Int? = null,
    artist: String? = null,
    album: String? = null,
    showEnqueueButton: Boolean = true,
    highlight: String? = null,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
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
                    position = position,
                    discNumber = discNumber,
                    showAlbumArt = true,
                    onClick = { isExpanded = false },
                    onPlayPauseClick = onPlayPauseClick,
                    onEnqueueClick = onEnqueueClick,
                    onGotoArtistClick = onGotoArtistClick,
                    onGotoAlbumClick = onGotoAlbumClick,
                    albumArt = albumArt,
                    highlight = highlight,
                )
            } else {
                LargeSongRowContent(
                    modifier = modifier.clickable { isExpanded = !isExpanded },
                    song = song,
                    isCurrentSong = isCurrentSong,
                    playerState = playerState,
                    albumArt = albumArt,
                    albumArtModifier = albumArtModifier,
                    position = position,
                    discNumber = discNumber,
                    artist = artist,
                    album = album,
                    showEnqueueButton = showEnqueueButton,
                    highlight = highlight,
                    onPlayPauseClick = onPlayPauseClick,
                    onEnqueueClick = onEnqueueClick,
                )
            }
        }
    }
}
