package us.huseli.umpc.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import us.huseli.retaintheme.compose.AlternatingTable
import us.huseli.retaintheme.compose.AlternatingTableScope
import us.huseli.umpc.BuildConfig
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.compose.NotConnectedToMPD
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.InfoViewModel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun InfoScreen(modifier: Modifier = Modifier, viewModel: InfoViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val connectedServer by viewModel.connectedServer.collectAsStateWithLifecycle()

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        if (connectedServer == null) NotConnectedToMPD()

        Text(
            stringResource(R.string.some_stats),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(10.dp),
        )

        AlternatingTable(cellWeights = listOf(0.5f)) {
            InfoRow(stringResource(R.string.number_of_artists), stats?.artists?.toString())
            InfoRow(stringResource(R.string.number_of_albums), stats?.albums?.toString())
            InfoRow(stringResource(R.string.number_of_songs), stats?.songs?.toString())
            InfoRowCountUpSeconds(stringResource(R.string.server_uptime), stats?.uptime)
            InfoRowCountUpSeconds(
                stringResource(R.string.play_time_since_restart),
                stats?.playtime,
                playerState == PlayerState.PLAY
            )
            InfoRow(stringResource(R.string.total_library_play_time), stats?.dbPlaytime?.toString())
            InfoRow(stringResource(R.string.mpd_version), connectedServer?.protocolVersion?.toString())
            InfoRow(stringResource(R.string.app_version), BuildConfig.VERSION_NAME)
        }

        if (BuildConfig.DEBUG) {
            Text(stringResource(R.string.debug), style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.padding(10.dp)) {
                Row {
                    OutlinedButton(
                        onClick = {
                            viewModel.addMessage(SnackbarMessage(message = "Message", actionLabel = "Action"))
                        },
                        content = { Text("Show message") },
                    )
                    OutlinedButton(
                        onClick = { viewModel.addError(SnackbarMessage(message = "Error", actionLabel = "Action")) },
                        content = { Text("Show error") },
                    )
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "background", MaterialTheme.colorScheme.background)
                    ColorSample(Modifier.weight(0.5f), "error", MaterialTheme.colorScheme.error)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "errorContainer", MaterialTheme.colorScheme.errorContainer)
                    ColorSample(Modifier.weight(0.5f), "inverseOnSurface", MaterialTheme.colorScheme.inverseOnSurface)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "inversePrimary", MaterialTheme.colorScheme.inversePrimary)
                    ColorSample(Modifier.weight(0.5f), "inverseSurface", MaterialTheme.colorScheme.inverseSurface)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "onBackground", MaterialTheme.colorScheme.onBackground)
                    ColorSample(Modifier.weight(0.5f), "onError", MaterialTheme.colorScheme.onError)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "onErrorContainer", MaterialTheme.colorScheme.onErrorContainer)
                    ColorSample(Modifier.weight(0.5f), "onPrimary", MaterialTheme.colorScheme.onPrimary)
                }
                Row {
                    ColorSample(
                        Modifier.weight(0.5f),
                        "onPrimaryContainer",
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ColorSample(Modifier.weight(0.5f), "onSecondary", MaterialTheme.colorScheme.onSecondary)
                }
                Row {
                    ColorSample(
                        Modifier.weight(0.5f),
                        "onSecondaryContainer",
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ColorSample(Modifier.weight(0.5f), "onSurface", MaterialTheme.colorScheme.onSurface)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "onSurfaceVariant", MaterialTheme.colorScheme.onSurfaceVariant)
                    ColorSample(Modifier.weight(0.5f), "onTertiary", MaterialTheme.colorScheme.onTertiary)
                }
                Row {
                    ColorSample(
                        Modifier.weight(0.5f),
                        "onTertiaryContainer",
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    ColorSample(Modifier.weight(0.5f), "outline", MaterialTheme.colorScheme.outline)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "outlineVariant", MaterialTheme.colorScheme.outlineVariant)
                    ColorSample(Modifier.weight(0.5f), "primary", MaterialTheme.colorScheme.primary)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "primaryContainer", MaterialTheme.colorScheme.primaryContainer)
                    ColorSample(Modifier.weight(0.5f), "scrim", MaterialTheme.colorScheme.scrim)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "secondary", MaterialTheme.colorScheme.secondary)
                    ColorSample(
                        Modifier.weight(0.5f),
                        "secondaryContainer",
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "surface", MaterialTheme.colorScheme.surface)
                    ColorSample(Modifier.weight(0.5f), "surfaceTint", MaterialTheme.colorScheme.surfaceTint)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "surfaceVariant", MaterialTheme.colorScheme.surfaceVariant)
                    ColorSample(Modifier.weight(0.5f), "tertiary", MaterialTheme.colorScheme.tertiary)
                }
                Row {
                    ColorSample(Modifier.weight(0.5f), "tertiaryContainer", MaterialTheme.colorScheme.tertiaryContainer)
                }
            }
        }
    }
}

@Composable
fun AlternatingTableScope.InfoRow(title: String, value: String?) {
    if (value != null) {
        Row {
            Cell { Text(title) }
            Cell { Text(value, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun AlternatingTableScope.InfoRowCountUpSeconds(title: String, initialValue: Duration?, count: Boolean = true) {
    if (initialValue != null) {
        var duration by remember(initialValue) { mutableStateOf(initialValue) }

        if (count) {
            LaunchedEffect(initialValue) {
                while (true) {
                    delay(1000)
                    duration += 1.toDuration(DurationUnit.SECONDS)
                }
            }
        }

        Row {
            Cell { Text(title) }
            Cell { Text(duration.toString(), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ColorSample(modifier: Modifier = Modifier, name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(modifier = Modifier.height(40.dp).width(40.dp).border(1.dp, Color.Black).background(color))
        Text(name, style = MaterialTheme.typography.labelSmall)
    }
}
