package us.huseli.umpc.compose.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.sharp.Done
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import us.huseli.umpc.PlaylistType
import us.huseli.umpc.R
import us.huseli.umpc.compose.DeletePlaylistDialog
import us.huseli.umpc.compose.EditDynamicPlaylistDialog
import us.huseli.umpc.compose.utils.SubMenuScreen
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.viewmodels.PlaylistListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistListViewModel = hiltViewModel(),
    onGotoStoredPlaylistClick: (MPDPlaylist) -> Unit,
    storedPlaylistScrollState: ScrollState = rememberScrollState(),
    dynamicPlaylistScrollState: ScrollState = rememberScrollState(),
) {
    val context = LocalContext.current
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()
    val protocolVersion by viewModel.protocolVersion.collectAsStateWithLifecycle()
    var isCreateDynamicPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editingDynamicPlaylist by rememberSaveable { mutableStateOf<DynamicPlaylist?>(null) }
    var deletingDynamicPlaylist by rememberSaveable { mutableStateOf<DynamicPlaylist?>(null) }

    if (isCreateDynamicPlaylistDialogOpen) {
        EditDynamicPlaylistDialog(
            protocolVerion = protocolVersion,
            onSave = { filter, shuffle ->
                isCreateDynamicPlaylistDialogOpen = false
                viewModel.createDynamicPlaylist(filter, shuffle)
            },
            onCancel = { isCreateDynamicPlaylistDialogOpen = false },
        )
    } else if (editingDynamicPlaylist != null) {
        editingDynamicPlaylist?.let { playlist ->
            EditDynamicPlaylistDialog(
                playlist = playlist,
                protocolVerion = protocolVersion,
                onSave = { filter, shuffle ->
                    isCreateDynamicPlaylistDialogOpen = false
                    viewModel.updateDynamicPlaylist(playlist, filter, shuffle)
                },
                onCancel = { editingDynamicPlaylist = null },
            )
        }
    } else if (deletingDynamicPlaylist != null) {
        deletingDynamicPlaylist?.let { playlist ->
            DeletePlaylistDialog(
                onConfirm = {
                    deletingDynamicPlaylist = null
                    viewModel.deleteDynamicPlaylist(playlist)
                    viewModel.addMessage(context.getString(R.string.playlist_was_deleted))
                },
                onCancel = { deletingDynamicPlaylist = null },
            )
        }
    }

    SubMenuScreen(
        modifier = modifier,
        menu = {
            FilterChip(
                shape = ShapeDefaults.ExtraSmall,
                selected = displayType == PlaylistType.STORED,
                onClick = { viewModel.setDisplayType(PlaylistType.STORED) },
                label = { Text(stringResource(R.string.stored_playlists)) },
                leadingIcon = {
                    if (displayType == PlaylistType.STORED) Icon(Icons.Sharp.Done, null)
                },
            )
            FilterChip(
                shape = ShapeDefaults.ExtraSmall,
                selected = displayType == PlaylistType.DYNAMIC,
                onClick = { viewModel.setDisplayType(PlaylistType.DYNAMIC) },
                label = { Text(stringResource(R.string.dynamic_playlists)) },
                leadingIcon = {
                    if (displayType == PlaylistType.DYNAMIC) Icon(Icons.Sharp.Done, null)
                },
            )
        }
    ) {
        when (displayType) {
            PlaylistType.STORED -> {
                val storedPlaylists by viewModel.storedPlaylists.collectAsStateWithLifecycle()

                Column(modifier = Modifier.fillMaxWidth().verticalScroll(storedPlaylistScrollState)) {
                    storedPlaylists.forEach { playlist ->
                        var soungCount by rememberSaveable { mutableStateOf<Int?>(null) }

                        LaunchedEffect(playlist) {
                            viewModel.getStoredPlaylistSongCount(playlist.name) { soungCount = it }
                        }

                        StoredPlaylistRow(
                            playlist = playlist,
                            songCount = soungCount,
                            onClick = { onGotoStoredPlaylistClick(playlist) },
                        )
                    }
                }
            }
            PlaylistType.DYNAMIC -> {
                val dynamicPlaylists by viewModel.dynamicPlaylists.collectAsStateWithLifecycle()
                var isHelpDialogOpen by rememberSaveable { mutableStateOf(false) }

                if (isHelpDialogOpen) DynamicPlaylistHelpDialog { isHelpDialogOpen = false }

                Column(
                    modifier = modifier.fillMaxWidth().verticalScroll(dynamicPlaylistScrollState)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                    ) {
                        OutlinedIconButton(
                            onClick = { isHelpDialogOpen = true },
                            content = {
                                Icon(Icons.Sharp.QuestionMark, null, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier.size(32.dp)
                        )
                        FilledTonalButton(
                            onClick = { isCreateDynamicPlaylistDialogOpen = true },
                            shape = ShapeDefaults.ExtraSmall,
                            content = { Text(stringResource(R.string.create_dynamic_playlist)) },
                        )
                    }
                    dynamicPlaylists.forEach { playlist ->
                        DynamicPlaylistRow(
                            onEditClick = { editingDynamicPlaylist = playlist },
                            onDeleteClick = { deletingDynamicPlaylist = playlist },
                            onPlayClick = { viewModel.activateDynamicPlaylist(playlist) },
                            playlist = playlist,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicPlaylistRow(
    modifier: Modifier = Modifier,
    playlist: DynamicPlaylist,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    Divider()
    Row(
        modifier = modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (playlist.songCount != null) {
            Column {
                Text(playlist.filter.toString())
                Text(
                    text = pluralStringResource(R.plurals.x_songs, playlist.songCount, playlist.songCount),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else Text(playlist.filter.toString())
        Row {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Sharp.Edit, stringResource(R.string.edit_dynamic_playlist))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Sharp.Delete, stringResource(R.string.delete_dynamic_playlist))
            }
            IconButton(onClick = onPlayClick) {
                Icon(Icons.Sharp.PlayArrow, stringResource(R.string.play_dynamic_playlist))
            }
        }
    }
}

@Composable
fun StoredPlaylistRow(
    modifier: Modifier = Modifier,
    playlist: MPDPlaylist,
    songCount: Int?,
    onClick: () -> Unit,
) {
    Divider()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(playlist.name)
        if (songCount != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pluralStringResource(R.plurals.x_songs, songCount, songCount),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DynamicPlaylistHelpDialog(modifier: Modifier = Modifier, onClose: () -> Unit) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose, content = { Text(stringResource(R.string.ok)) }) },
        title = { Text(stringResource(R.string.about_dynamic_playlists)) },
        shape = ShapeDefaults.ExtraSmall,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.dynamic_playlist_help1))
                Text(stringResource(R.string.dynamic_playlist_help2))
                Text(stringResource(R.string.dynamic_playlist_help3))
                Text(stringResource(R.string.dynamic_playlist_help4))
            }
        },
    )
}
