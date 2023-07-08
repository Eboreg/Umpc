package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter

@Composable
fun EditDynamicPlaylistDialog(
    modifier: Modifier = Modifier,
    playlist: DynamicPlaylist? = null,
    onSave: (String, DynamicPlaylistFilter, Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(playlist?.name ?: "") }
    var filter by rememberSaveable { mutableStateOf(playlist?.filter ?: DynamicPlaylistFilter()) }
    var shuffle by rememberSaveable { mutableStateOf(playlist?.shuffle ?: false) }

    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(stringResource(R.string.create_dynamic_playlist)) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = { onSave(name, filter, shuffle) }, enabled = name.isNotEmpty()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.name)) },
                )
                DynamicPlaylistFilterSection(filter = filter, onChange = { filter = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.padding(start = 0.dp),
                        checked = shuffle,
                        onCheckedChange = { shuffle = it },
                    )
                    Text(stringResource(R.string.shuffle))
                }
            }
        }
    )
}
