package us.huseli.umpc.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.formatDateTime
import us.huseli.umpc.viewmodels.MPDViewModel

@Composable
fun PlaylistListScreen(
    modifier: Modifier = Modifier,
    viewModel: MPDViewModel = hiltViewModel(),
    onGotoPlaylistClick: (MPDPlaylist) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth().verticalScroll(scrollState)) {
        playlists.forEach { playlist ->
            PlaylistRow(
                playlist = playlist,
                onClick = { onGotoPlaylistClick(playlist) },
            )
        }
    }
}

@Composable
fun PlaylistRow(
    modifier: Modifier = Modifier,
    playlist: MPDPlaylist,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(playlist.name)
        Column(horizontalAlignment = Alignment.End) {
            Text(playlist.lastModified?.formatDateTime() ?: "-", style = MaterialTheme.typography.bodySmall)
        }
    }
    Divider()
}
