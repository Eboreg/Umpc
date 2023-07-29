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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDPlaylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    modifier: Modifier = Modifier,
    title: String,
    playlists: List<MPDPlaylist>,
    allowExistingPlaylist: Boolean = true,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<MPDPlaylist?>(null) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    val nameIsDuplicate = playlists.map { it.name }.contains(newPlaylistName)
    val isValid =
        if (allowExistingPlaylist) selectedPlaylist?.name?.isNotEmpty() == true || newPlaylistName.isNotEmpty()
        else newPlaylistName.isNotEmpty() && !nameIsDuplicate
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
                if (allowExistingPlaylist) {
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
                }

                Text(stringResource(if (allowExistingPlaylist) R.string.or_enter_the_name_of_a_new_playlist else R.string.enter_the_name_of_a_new_playlist))
                OutlinedTextField(
                    value = newPlaylistName,
                    isError = !allowExistingPlaylist && nameIsDuplicate,
                    onValueChange = {
                        newPlaylistName = it
                        if (it.isNotEmpty()) selectedPlaylist = null
                    },
                    singleLine = true,
                    supportingText = {
                        if (!allowExistingPlaylist && nameIsDuplicate)
                            Text(stringResource(R.string.a_playlist_with_this_name_already_exists))
                    }
                )
            }
        }
    )
}
