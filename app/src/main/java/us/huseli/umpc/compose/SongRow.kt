package us.huseli.umpc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.mpd.highlight

@Composable
fun SongRow(
    modifier: Modifier = Modifier,
    title: String,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
    height: Dp = 54.dp,
    artist: String? = null,
    album: String? = null,
    position: Int? = null,
    discNumber: Int? = null,
    duration: Double? = null,
    year: Int? = null,
    showAlbumArt: Boolean = true,
    albumArt: ImageBitmap? = null,
    highlight: String? = null,
) {
    val titleRow =
        if (discNumber != null && position != null) "${discNumber}-${position}. $title"
        else if (position != null) "${position}. $title"
        else title
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
    var rowModifier = if (isCurrentSong) modifier.background(MaterialTheme.colorScheme.surfaceVariant) else modifier
    rowModifier = rowModifier
        .fillMaxWidth()
        .clickable { isMenuVisible = !isMenuVisible }
        .padding(vertical = 8.dp)
        .height(height)

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAlbumArt) {
            AlbumArt(
                imageBitmap = albumArt,
                modifier = Modifier.padding(end = 8.dp).fillMaxHeight(),
                altIcon = Icons.Sharp.MusicNote
            )
        }
        Column(
            modifier = Modifier.padding(start = if (showAlbumArt) 0.dp else 8.dp).weight(1f),
        ) {
            AutoScrollingTextLine(text = titleRow.highlight(highlight))
            if (artist != null) {
                AutoScrollingTextLine(
                    text = artist.highlight(highlight),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (album != null) {
                AutoScrollingTextLine(
                    text = album.highlight(highlight),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.width(IntrinsicSize.Min).padding(end = 8.dp),
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
            IconButton(onClick = onEnqueueClick) {
                Icon(Icons.Sharp.QueueMusic, stringResource(R.string.enqueue))
            }
            IconButton(onClick = onPlayPauseClick) {
                if (isPlaying) Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
            }
        }
    }

    if (isMenuVisible) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            onGotoAlbumClick?.let { onClick ->
                OutlinedButton(
                    onClick = onClick,
                    shape = ShapeDefaults.ExtraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    content = { Text(stringResource(R.string.go_to_album)) },
                )
            }
            onGotoArtistClick?.let { onClick ->
                OutlinedButton(
                    onClick = onClick,
                    shape = ShapeDefaults.ExtraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    content = { Text(stringResource(R.string.go_to_artist)) },
                )
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
    val isCurrentSong = song.filename == currentSongFilename

    SongRow(
        modifier = modifier,
        title = song.title,
        artist = song.artist.takeIf { it != album?.artist },
        position = song.trackNumber,
        discNumber = song.discNumber,
        duration = song.duration,
        showAlbumArt = false,
        isCurrentSong = isCurrentSong,
        year = if (album?.yearRange?.first != album?.yearRange?.last) song.year else null,
        height = 40.dp,
        isPlaying = isCurrentSong && playerState == PlayerState.PLAY,
        onPlayPauseClick = onPlayPauseClick,
        onEnqueueClick = onEnqueueClick,
        onGotoArtistClick = onGotoArtistClick,
        onGotoAlbumClick = onGotoAlbumClick,
    )
}
