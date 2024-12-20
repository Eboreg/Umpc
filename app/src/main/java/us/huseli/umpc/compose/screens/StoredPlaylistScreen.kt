package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.compose.DeletePlaylistDialog
import us.huseli.umpc.compose.LargeSongRowList
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDateTime
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.StoredPlaylistViewModel

@Composable
fun StoredPlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: StoredPlaylistViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onGotoQueueClick: () -> Unit,
    onAddSongToPlaylistClick: (MPDSong) -> Unit,
    onPlaylistDeleted: () -> Unit,
    onPlaylistRenamed: (String) -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val playlist by viewModel.playlist.collectAsStateWithLifecycle(null)
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    var isRenameDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }

    val addRemovedSongsMessage: (Int) -> Unit = { songCount ->
        viewModel.addMessage(
            SnackbarMessage(
                message = context.resources.getQuantityString(
                    R.plurals.removed_x_songs_from_playlist,
                    songCount,
                    songCount
                ),
                actionLabel = context.getString(R.string.undo),
                onActionPerformed = { viewModel.undoRemoveSongs() },
            )
        )
    }

    if (isRenameDialogOpen) {
        playlist?.let {
            RenameStoredPlaylistDialog(
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
            DeletePlaylistDialog(
                name = it.name,
                onConfirm = {
                    viewModel.delete { response ->
                        if (response.isSuccess) {
                            viewModel.addMessage(context.getString(R.string.playlist_was_deleted))
                            onPlaylistDeleted()
                        } else {
                            response.error?.let { error -> viewModel.addError(error) }
                        }
                    }
                },
                onCancel = { isDeleteDialogOpen = false },
            )
        }
    }

    LargeSongRowList(
        modifier = modifier,
        viewModel = viewModel,
        songs = playlist?.songs ?: emptyList(),
        listState = viewModel.listState,
        currentSong = currentSong,
        reorderable = true,
        removable = true,
        showSongPositions = true,
        onMoveSong = { from, to -> viewModel.moveSong(from, to) },
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        onGotoQueueClick = onGotoQueueClick,
        onGotoPlaylistClick = onGotoPlaylistClick,
        onAddSongToPlaylistClick = onAddSongToPlaylistClick,
        onRemoveSong = { song ->
            viewModel.removeSong(song)
            addRemovedSongsMessage(1)
        },
        onRemoveSelectedSongs = {
            val songCount = viewModel.selectedSongs.value.size
            viewModel.removeSelectedSongs()
            addRemovedSongsMessage(songCount)
        },
        onPlaySongClick = { viewModel.playSongs(playlist?.songs ?: emptyList(), it) },
        subMenu = {
            playlist?.let {
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(start = 10.dp).padding(vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(it.name, style = MaterialTheme.typography.headlineMedium)

                            Row {
                                IconButton(
                                    onClick = { isRenameDialogOpen = true },
                                    content = { Icon(Icons.Sharp.Edit, stringResource(R.string.rename)) },
                                )
                                IconButton(
                                    onClick = { isDeleteDialogOpen = true },
                                    content = { Icon(Icons.Sharp.Delete, stringResource(R.string.delete)) },
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.enqueue { response ->
                                            if (response.isSuccess) viewModel.addMessage(
                                                SnackbarMessage(
                                                    message = context.getString(
                                                        R.string.playlist_x_was_enqueued,
                                                        it.name,
                                                    ),
                                                    actionLabel = context.getString(R.string.go_to_queue),
                                                    onActionPerformed = onGotoQueueClick,
                                                )
                                            )
                                            else viewModel.addError(
                                                context.getString(
                                                    R.string.could_not_enqueue_playlist,
                                                    response.error ?: context.getString(R.string.unknown_error),
                                                )
                                            )
                                        }
                                    },
                                    content = { Icon(Icons.Sharp.PlaylistPlay, stringResource(R.string.enqueue)) },
                                )
                                IconButton(
                                    onClick = { viewModel.play() },
                                    content = { Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play)) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            it.songs?.let { songs ->
                                Text(
                                    text = pluralStringResource(R.plurals.x_songs, songs.size, songs.size),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            it.lastModified?.let { lastModified ->
                                Text(
                                    text = stringResource(R.string.last_modified, lastModified.formatDateTime()),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun RenameStoredPlaylistDialog(
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
