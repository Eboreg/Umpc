package us.huseli.umpc.compose.screens

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.viewmodels.PlaylistListViewModel

@Composable
fun PlaylistListScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistListViewModel = hiltViewModel(),
    onGotoPlaylistClick: (MPDPlaylist) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val storedPlaylists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    val displayType by viewModel.displayType.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth().verticalScroll(scrollState)) {
        storedPlaylists.forEach { playlist ->
            var soungCount by rememberSaveable { mutableStateOf<Int?>(null) }

            LaunchedEffect(playlist) {
                viewModel.getStoredPlaylistSongCount(playlist.name) { soungCount = it }
            }

            PlaylistRow(
                playlist = playlist,
                songCount = soungCount,
                onClick = { onGotoPlaylistClick(playlist) },
            )
        }
    }
}

@Composable
fun PlaylistRow(
    modifier: Modifier = Modifier,
    playlist: MPDPlaylist,
    songCount: Int?,
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
        if (songCount != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = pluralStringResource(R.plurals.x_songs, songCount, songCount),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    Divider()
}
