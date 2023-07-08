package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.compose.LargeSongRow
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDateTime
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.PlaylistViewModel

@Composable
fun RenamePlaylistDialog(
    modifier: Modifier = Modifier,
    name: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var newName by rememberSaveable(name) { mutableStateOf(name) }

    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(stringResource(R.string.rename_playlist)) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }, enabled = newName.isNotBlank()) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text(stringResource(R.string.new_name)) },
            )
        }
    )
}

@Composable
fun DeletePlaylistDialog(
    modifier: Modifier = Modifier,
    name: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(stringResource(R.string.delete_playlist)) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        text = {
            Text(stringResource(R.string.delete_playlist_x, name))
        }
    )
}

@Composable
fun PlaylistScreenMetaInfo(playlist: MPDPlaylist, songs: List<MPDSong>) {
    Text(
        pluralStringResource(R.plurals.x_songs, songs.size, songs.size),
        style = MaterialTheme.typography.bodySmall
    )
    playlist.lastModified?.let { lastModified ->
        Text(
            stringResource(R.string.last_modified, lastModified.formatDateTime()),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onPlaylistDeleted: () -> Unit,
    onPlaylistRenamed: (String) -> Unit,
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle(null)
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    var isRenameDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isRenameDialogOpen) {
        playlist?.let {
            RenamePlaylistDialog(
                name = it.name,
                onConfirm = { newName ->
                    viewModel.rename(newName) { isSuccess ->
                        if (isSuccess) onPlaylistRenamed(newName)
                    }
                },
                onCancel = { isRenameDialogOpen = false },
            )
        }
    }

    if (isDeleteDialogOpen) {
        playlist?.let {
            val successMessage = stringResource(R.string.the_playlist_was_deleted)

            DeletePlaylistDialog(
                name = it.name,
                onConfirm = {
                    viewModel.deletePlaylist { response ->
                        if (response.isSuccess) {
                            viewModel.addMessage(successMessage)
                            onPlaylistDeleted()
                        } else {
                            response.error?.let { error -> viewModel.addMessage(error) }
                        }
                    }
                },
                onCancel = { isDeleteDialogOpen = false },
            )
        }
    }

    Column(modifier = modifier) {
        playlist?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (isInLandscapeMode()) {
                    Text(it.name, style = MaterialTheme.typography.headlineMedium)
                    Column {
                        PlaylistScreenMetaInfo(it, songs)
                    }
                } else {
                    Column {
                        Text(it.name, style = MaterialTheme.typography.headlineMedium)
                        PlaylistScreenMetaInfo(it, songs)
                    }
                }
                Row {
                    IconButton(onClick = { isRenameDialogOpen = true }) {
                        Icon(Icons.Sharp.Edit, stringResource(R.string.rename))
                    }
                    IconButton(onClick = { isDeleteDialogOpen = true }) {
                        Icon(Icons.Sharp.Delete, stringResource(R.string.delete))
                    }
                    IconButton(onClick = { viewModel.play() }) {
                        Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play))
                    }
                }
            }
        }
        LazyColumn(state = listState) {
            items(songs) { song ->
                val albumArt by viewModel.getAlbumArtState(song)

                Divider()
                PlaylistSongRow(
                    song = song,
                    currentSongFilename = currentSongFilename,
                    playerState = playerState,
                    albumArt = albumArt,
                    onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                    onEnqueueClick = { viewModel.enqueueSong(song) },
                    onGotoArtistClick = { onGotoArtistClick(song.artist) },
                    onGotoAlbumClick = { onGotoAlbumClick(song.album) },
                )
            }
        }
    }
}

@Composable
fun PlaylistSongRow(
    modifier: Modifier = Modifier,
    song: MPDSong,
    currentSongFilename: String?,
    playerState: PlayerState?,
    onPlayPauseClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onGotoAlbumClick: () -> Unit,
    onGotoArtistClick: () -> Unit,
    albumArt: ImageBitmap?,
) {
    LargeSongRow(
        modifier = modifier,
        song = song,
        isCurrentSong = currentSongFilename == song.filename,
        playerState = playerState,
        onPlayPauseClick = onPlayPauseClick,
        onEnqueueClick = onEnqueueClick,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        artist = song.artist,
        album = song.album.name,
        albumArt = albumArt,
    )
}
