package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.compose.utils.SimpleResponsiveBlock
import us.huseli.umpc.data.MPDServerCredentials
import us.huseli.umpc.viewmodels.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val outputs by viewModel.outputs.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val serverIdx by viewModel.selectedServerIdx.collectAsStateWithLifecycle()
    val fetchSpotifyAlbumArt by viewModel.fetchSpotifyAlbumArt.collectAsStateWithLifecycle()

    var isServerDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var isServerDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteServerDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editingServerIdx by rememberSaveable { mutableStateOf(serverIdx) }

    if (isServerDialogOpen) {
        ServerSettingsDialog(
            server = editingServerIdx?.let { servers[it] } ?: MPDServerCredentials(hostname = "", port = 6600),
            title = stringResource(editingServerIdx?.let { R.string.update_mpd_server } ?: R.string.add_mpd_server),
            onCancel = { isServerDialogOpen = false },
            onConfirm = { updatedServer ->
                editingServerIdx?.also {
                    viewModel.updateServer(it, updatedServer)
                } ?: run {
                    viewModel.addServer(updatedServer)
                }
                isServerDialogOpen = false
            },
        )
    }

    if (isDeleteServerDialogOpen) {
        serverIdx?.let { idx ->
            servers.getOrNull(idx)?.let { server ->
                DeleteServerDialog(
                    server = server,
                    onConfirm = {
                        viewModel.deleteServer(idx)
                        isDeleteServerDialogOpen = false
                    },
                    onCancel = { isDeleteServerDialogOpen = false },
                )
            }
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SimpleResponsiveBlock(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalDistance = 20.dp,
            horizontalDistance = 20.dp,
            content1 = {
                /** MPD server config: */
                Text(
                    text = stringResource(R.string.server_settings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                Row {
                    /**
                     * The combination of menuAnchor() and exposedDropdownSize()
                     * SHOULD mean that the ExposedDropdownMenu gets the same
                     * width as the TextField, but it doesn't. Android bug?
                     */
                    ExposedDropdownMenuBox(
                        expanded = isServerDropdownExpanded,
                        onExpandedChange = { isServerDropdownExpanded = !isServerDropdownExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = serverIdx?.let { servers.getOrNull(it)?.toString() } ?: stringResource(
                                if (servers.isEmpty()) R.string.no_servers_added_yet
                                else R.string.no_server_selected
                            ),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.mpd_server)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isServerDropdownExpanded) },
                        )
                        ExposedDropdownMenu(
                            expanded = isServerDropdownExpanded,
                            onDismissRequest = { isServerDropdownExpanded = false },
                            modifier = Modifier.exposedDropdownSize(),
                        ) {
                            servers.forEachIndexed { index, server ->
                                DropdownMenuItem(
                                    text = { Text(server.toString()) },
                                    onClick = {
                                        viewModel.selectServer(index)
                                        isServerDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(
                        onClick = {
                            serverIdx?.let {
                                editingServerIdx = it
                                isServerDialogOpen = true
                            }
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                        content = { Text(stringResource(R.string.update)) },
                        enabled = serverIdx != null,
                    )
                    OutlinedButton(
                        onClick = { isDeleteServerDialogOpen = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        ),
                        shape = MaterialTheme.shapes.extraSmall,
                        content = { Text(stringResource(R.string.delete)) },
                        enabled = serverIdx != null,
                    )
                    OutlinedButton(
                        onClick = {
                            editingServerIdx = null
                            isServerDialogOpen = true
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                        content = { Text(stringResource(R.string.add_new)) },
                    )
                }
            },
            content2 = {
                if (outputs.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.outputs),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    outputs.forEach { output ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                modifier = Modifier.padding(start = 0.dp),
                                checked = output.isEnabled,
                                onCheckedChange = { viewModel.setOutputEnabled(output.id, it) }
                            )
                            Text(output.name)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text = stringResource(R.string.various),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.padding(start = 0.dp),
                        checked = fetchSpotifyAlbumArt,
                        onCheckedChange = { viewModel.setFetchSpotifyAlbumArt(it) }
                    )
                    Text(stringResource(R.string.fetch_missing_album_art_from_spotify))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAlbumArtCache {
                                viewModel.addMessage(
                                    context.getString(R.string.all_locally_stored_album_art_was_cleared)
                                )
                            }
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                        content = { Text(stringResource(R.string.clear_album_art_cache)) },
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.updateDatabase(
                                onFinish = {
                                    viewModel.addMessage(context.getString(R.string.database_update_has_started))
                                },
                                onUpdateFinish = {
                                    viewModel.addMessage(context.getString(R.string.database_update_finished))
                                }
                            )
                        },
                        enabled = isConnected,
                        shape = MaterialTheme.shapes.extraSmall,
                        content = { Text(stringResource(R.string.update_database)) },
                    )

                    Button(
                        onClick = {
                            viewModel.save()
                            viewModel.addMessage(context.getString(R.string.settings_saved))
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                        content = { Text(stringResource(R.string.save)) },
                    )
                }
            }
        )
    }
}

@Composable
fun DeleteServerDialog(
    modifier: Modifier = Modifier,
    server: MPDServerCredentials,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(stringResource(R.string.delete_mpd_server)) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(onClick = onConfirm, content = { Text(stringResource(R.string.delete)) })
        },
        dismissButton = {
            TextButton(onClick = onCancel, content = { Text(stringResource(R.string.cancel)) })
        },
        text = { Text(stringResource(R.string.do_you_want_to_delete_server, server)) },
    )
}

@Composable
fun ServerSettingsDialog(
    modifier: Modifier = Modifier,
    server: MPDServerCredentials,
    title: String,
    onConfirm: (MPDServerCredentials) -> Unit,
    onCancel: () -> Unit,
) {
    var hostname by rememberSaveable(server) { mutableStateOf(server.hostname) }
    var port by rememberSaveable(server) { mutableStateOf<Int?>(server.port) }
    var password by rememberSaveable(server) { mutableStateOf(server.password) }
    var streamingUrl by rememberSaveable(server) { mutableStateOf(server.streamingUrl) }
    val isValid = hostname.isNotBlank() && port?.takeIf { it > 0 } != null

    AlertDialog(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        title = { Text(title) },
        onDismissRequest = onCancel,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        MPDServerCredentials(
                            hostname = hostname,
                            port = port!!,
                            password = password,
                            streamingUrl = streamingUrl,
                        )
                    )
                },
                enabled = isValid,
                content = { Text(stringResource(R.string.save)) },
            )
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.weight(0.5f),
                        value = hostname,
                        onValueChange = { hostname = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.hostname)) },
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(0.2f),
                        value = port?.toString() ?: "",
                        onValueChange = { value ->
                            port = value.toIntOrNull()?.takeIf { it > 0 }
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.port)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password ?: "",
                    onValueChange = { password = it.takeIf { it.isNotBlank() } },
                    singleLine = true,
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = streamingUrl ?: "",
                    onValueChange = { streamingUrl = it.takeIf { it.isNotBlank() } },
                    singleLine = true,
                    label = { Text(stringResource(R.string.streaming_url)) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                )
            }
        }
    )
}
