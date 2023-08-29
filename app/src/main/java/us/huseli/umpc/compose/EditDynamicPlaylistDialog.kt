package us.huseli.umpc.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import us.huseli.umpc.R
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.data.MPDVersion

@Composable
fun EditDynamicPlaylistDialog(
    modifier: Modifier = Modifier,
    playlist: DynamicPlaylist? = null,
    protocolVerion: MPDVersion,
    title: String,
    onSave: (filters: List<DynamicPlaylistFilter>, shuffle: Boolean, operator: DynamicPlaylist.Operator) -> Unit,
    onCancel: () -> Unit,
) {
    var filters by rememberSaveable { mutableStateOf(playlist?.filters ?: emptyList()) }
    var operator by rememberSaveable { mutableStateOf(playlist?.operator ?: DynamicPlaylist.Operator.AND) }
    var shuffle by rememberSaveable { mutableStateOf(playlist?.shuffle ?: false) }
    var newFilter by rememberSaveable { mutableStateOf(DynamicPlaylistFilter()) }

    AlertDialog(
        modifier = modifier.padding(horizontal = 20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(title) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                enabled = filters.isNotEmpty(),
                onClick = { onSave(filters, shuffle, operator) },
                content = { Text(stringResource(R.string.save)) },
            )
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, ShapeDefaults.ExtraSmall)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge)
                    if (filters.isEmpty()) {
                        Text(stringResource(R.string.no_filters_defined))
                    }
                    filters.forEachIndexed { index, filter ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(filter.toString())
                            IconButton(
                                onClick = { filters -= filters[index] },
                                modifier = Modifier.size(40.dp, 30.dp),
                                content = {
                                    Icon(
                                        imageVector = Icons.Sharp.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline, ShapeDefaults.ExtraSmall)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_filter),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    DynamicPlaylistFilterSection(
                        filter = newFilter,
                        protocolVersion = protocolVerion,
                        onChange = { newFilter = it },
                        onAdd = {
                            filters += newFilter
                            newFilter = DynamicPlaylistFilter()
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(modifier = Modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.operator_colon))
                        DynamicPlaylist.Operator.values().forEach { op ->
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = op == operator,
                                        onClick = { operator = op },
                                        role = Role.RadioButton,
                                    )
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = op == operator, onClick = null)
                                Text(op.display, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }

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
        },
    )
}
