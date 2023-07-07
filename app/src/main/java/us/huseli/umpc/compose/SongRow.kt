package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbumWithSongs
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
            )
        } else {
            if (artist != null) {
                AutoScrollingTextLine(
                    text = artist.highlight(highlight),
                    fontSize = fontSize,
                    color = color,
                )
            }
            if (album != null) {
                AutoScrollingTextLine(
                    text = album.highlight(highlight),
                    fontSize = fontSize,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun SongRow(
    modifier: Modifier = Modifier,
    title: String,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
    artist: String? = null,
    album: String? = null,
    position: Int? = null,
    discNumber: Int? = null,
    duration: Double? = null,
    year: Int? = null,
    showAlbumArt: Boolean = true,
    albumArt: ImageBitmap? = null,
    highlight: String? = null,
    showEnqueueButton: Boolean = true,
    trailingContent: @Composable (RowScope.() -> Unit) = {},
) {
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
    val isClickable = onGotoAlbumClick != null || onGotoArtistClick != null
    var rowModifier = modifier.fillMaxWidth()
    val tonalElevation = LocalAbsoluteTonalElevation.current + if (isCurrentSong) 5.dp else 0.dp
    val titleRow =
        if (discNumber != null && position != null) "${discNumber}-${position}. $title"
        else if (position != null) "${position}. $title"
        else title
    if (isClickable) rowModifier = rowModifier.clickable { isMenuVisible = !isMenuVisible }

    CompositionLocalProvider(LocalAbsoluteTonalElevation provides tonalElevation) {
        Surface {
            Column(modifier = rowModifier) {
                Row(
                    modifier = modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showAlbumArt)
                        AlbumArt(imageBitmap = albumArt, altIcon = Icons.Sharp.MusicNote, forceSquare = true)

                    Column(
                        modifier = Modifier.padding(start = 8.dp).padding(vertical = 8.dp).weight(1f),
                    ) {
                        AutoScrollingTextLine(text = titleRow.highlight(highlight))
                        SongRowArtistAlbumInfo(artist, album, highlight)
                    }

                    Row(
                        modifier = Modifier.width(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            duration?.formatDuration()?.let {
                                Text(text = it, fontSize = 12.sp, textAlign = TextAlign.End)
                            }
                            year?.let {
                                Text(text = it.toString(), fontSize = 12.sp, textAlign = TextAlign.End)
                            }
                        }
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
                        trailingContent()
                    }
                }

                if (isMenuVisible) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        onGotoAlbumClick?.let { onClick ->
                            SmallOutlinedButton(
                                onClick = onClick,
                                content = { Text(stringResource(R.string.go_to_album)) },
                            )
                        }
                        onGotoArtistClick?.let { onClick ->
                            SmallOutlinedButton(
                                onClick = onClick,
                                content = { Text(stringResource(R.string.go_to_artist)) },
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AlbumSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    album: MPDAlbumWithSongs?,
    currentSongFilename: String?,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
) {
    /** A SongRow for use in an album hierarchy. */
    SongRow(
        modifier = modifier,
        title = song.title,
        isCurrentSong = song.filename == currentSongFilename,
        playerState = playerState,
        onPlayPauseClick = onPlayPauseClick,
        onEnqueueClick = onEnqueueClick,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        artist = song.artist.takeIf { it != album?.album?.artist },
        position = song.trackNumber,
        discNumber = song.discNumber,
        duration = song.duration,
        year = if (album?.yearRange?.first != album?.yearRange?.last) song.year else null,
        showAlbumArt = false,
    )
}
