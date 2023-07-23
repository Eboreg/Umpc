package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import us.huseli.umpc.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val outputs by viewModel.outputs.collectAsStateWithLifecycle()
    val hostname by viewModel.hostname.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val port by viewModel.port.collectAsStateWithLifecycle()
    val streamingUrl by viewModel.streamingUrl.collectAsStateWithLifecycle()

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SimpleResponsiveBlock(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalDistance = 20.dp,
            horizontalDistance = 20.dp,
            content1 = {
                Text(
                    text = stringResource(R.string.server_settings),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Row {
                    OutlinedTextField(
                        modifier = Modifier.weight(0.5f),
                        value = hostname,
                        onValueChange = { viewModel.setHostname(it) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.hostname)) },
                    )
                    OutlinedTextField(
                        modifier = Modifier.weight(0.2f),
                        value = port.toString(),
                        onValueChange = {
                            try {
                                viewModel.setPort(it.toInt())
                            } catch (e: NumberFormatException) {
                                viewModel.setPort(port)
                            }
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.port)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = password,
                    onValueChange = { viewModel.setPassword(it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = streamingUrl ?: "",
                    onValueChange = { viewModel.setStreamingUrl(it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.streaming_url)) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                )
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

                OutlinedButton(
                    onClick = {
                        viewModel.clearAlbumArtCache {
                            viewModel.addMessage(context.getString(R.string.all_locally_stored_album_art_was_cleared))
                        }
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(stringResource(R.string.clear_album_art_cache))
                }

                Button(
                    onClick = {
                        viewModel.save()
                        viewModel.addMessage(context.getString(R.string.settings_saved))
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        )
    }
}
