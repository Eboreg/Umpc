package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material.icons.sharp.InterpreterMode
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material.icons.sharp.Pause
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistPlay
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.compose.utils.AutoScrollingTextLine
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.mpd.highlight

@Composable
fun ExpandedSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    isCurrentSong: Boolean,
    playerState: PlayerState?,
    position: Int? = null,
    discNumber: Int? = null,
    showAlbumArt: Boolean = true,
    albumArt: ImageBitmap? = null,
    highlight: String? = null,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onGotoAlbumClick: (() -> Unit)? = null,
    onGotoArtistClick: (() -> Unit)? = null,
) {
    val tonalElevation = LocalAbsoluteTonalElevation.current + if (isCurrentSong) 5.dp else 0.dp
    val titleRow =
        if (discNumber != null && position != null) "${discNumber}-${position}. ${song.title}"
        else if (position != null) "${position}. ${song.title}"
        else song.title

    CompositionLocalProvider(LocalAbsoluteTonalElevation provides tonalElevation) {
        Surface {
            Row(modifier = modifier.height(IntrinsicSize.Min).fillMaxWidth().clickable { onClick() }) {
                if (showAlbumArt) {
                    AlbumArt(
                        imageBitmap = albumArt,
                        altIcon = Icons.Sharp.MusicNote,
                        forceSquare = true,
                    )
                }
                Column(
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    AutoScrollingTextLine(titleRow, modifier = Modifier.padding(end = 10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (isInLandscapeMode()) {
                                AutoScrollingTextLine(
                                    text = "${song.artist} • ${song.album.name}".highlight(highlight),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                AutoScrollingTextLine(
                                    song.artist.highlight(highlight),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                AutoScrollingTextLine(
                                    song.album.name.highlight(highlight),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            song.duration?.let {
                                Text(it.formatDuration(), style = MaterialTheme.typography.bodySmall)
                            }
                            song.year?.let {
                                Text(it.toString(), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        onGotoArtistClick?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Sharp.InterpreterMode, stringResource(R.string.go_to_artist))
                            }
                        }
                        onGotoAlbumClick?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Sharp.Album, stringResource(R.string.go_to_album))
                            }
                        }
                        IconButton(onClick = onEnqueueClick) {
                            Icon(Icons.Sharp.PlaylistPlay, stringResource(R.string.enqueue))
                        }
                        IconButton(onClick = onAddToPlaylistClick) {
                            Icon(Icons.Sharp.PlaylistAdd, stringResource(R.string.add_to_playlist))
                        }
                        IconButton(onClick = onPlayPauseClick) {
                            if (isCurrentSong && playerState == PlayerState.PLAY)
                                Icon(Icons.Sharp.Pause, stringResource(R.string.pause))
                            else Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
                        }
                    }
                }
            }
        }
    }
}
