package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.mpd.engine.SnackbarMessage
import us.huseli.umpc.mpd.response.MPDBatchMapResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    modifier: Modifier = Modifier,
    title: String,
    playlists: List<MPDPlaylist>,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<MPDPlaylist?>(null) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    val isValid = selectedPlaylist?.name?.isNotEmpty() == true || newPlaylistName.isNotEmpty()
    val selectedPlaylistName = selectedPlaylist?.name ?: newPlaylistName.takeIf { it.isNotEmpty() }

    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                onClick = { selectedPlaylistName?.let { onConfirm(it) } },
                enabled = isValid,
                content = { Text(stringResource(R.string.ok)) },
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.add_x_to_playlist, title))
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor(),
                        readOnly = true,
                        value = selectedPlaylist?.name ?: "",
                        onValueChange = {},
                        label = { Text(stringResource(R.string.select_a_playlist)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                    ) {
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                onClick = {
                                    selectedPlaylist = playlist
                                    isDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Text(stringResource(R.string.or_enter_the_name_of_a_new_playlist))
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = {
                        newPlaylistName = it
                        if (it.isNotEmpty()) selectedPlaylist = null
                    },
                    singleLine = true,
                )
            }
        }
    )
}

@Composable
fun BatchAddToPlaylistDialog(
    modifier: Modifier = Modifier,
    itemType: AddToPlaylistItemType,
    itemCount: Int,
    playlists: List<MPDPlaylist>,
    addFunction: (String, (MPDBatchMapResponse) -> Unit) -> Unit,
    addMessage: (SnackbarMessage) -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
    closeDialog: () -> Unit,
) {
    val context = LocalContext.current

    AddToPlaylistDialog(
        modifier = modifier,
        title = when (itemType) {
            AddToPlaylistItemType.SONG -> pluralStringResource(R.plurals.x_songs, itemCount, itemCount)
            AddToPlaylistItemType.ALBUM -> pluralStringResource(R.plurals.x_albums, itemCount, itemCount)
        },
        playlists = playlists,
        onConfirm = {
            addFunction(it) { response ->
                if (response.successCount == 0 && response.errorCount > 0) addMessage(
                    SnackbarMessage(
                        message = context.resources.getQuantityString(
                            when (itemType) {
                                AddToPlaylistItemType.SONG -> R.plurals.add_songs_playlist_fail
                                AddToPlaylistItemType.ALBUM -> R.plurals.add_albums_playlist_fail
                            },
                            response.errorCount,
                            response.errorCount
                        ),
                    )
                )
                else if (response.successCount > 0 && response.errorCount == 0) addMessage(
                    SnackbarMessage(
                        message = context.resources.getQuantityString(
                            when (itemType) {
                                AddToPlaylistItemType.SONG -> R.plurals.add_songs_playlist_success
                                AddToPlaylistItemType.ALBUM -> R.plurals.add_albums_playlist_success
                            },
                            response.successCount,
                            response.successCount
                        ),
                        actionLabel = context.getString(R.string.go_to_playlist),
                        onActionPerformed = { onGotoPlaylistClick(it) },
                    )
                )
                else addMessage(
                    SnackbarMessage(
                        message = context.getString(
                            when (itemType) {
                                AddToPlaylistItemType.SONG -> R.string.add_songs_playlist_success_and_fail
                                AddToPlaylistItemType.ALBUM -> R.string.add_albums_playlist_success_and_fail
                            },
                            response.successCount,
                            response.errorCount
                        ),
                        actionLabel = context.getString(R.string.go_to_playlist),
                        onActionPerformed = { onGotoPlaylistClick(it) },
                    )
                )
            }
            closeDialog()
        },
        onCancel = closeDialog,
    )
}
