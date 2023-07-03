package us.huseli.umpc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.MPDViewModel

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: MPDViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDSong) -> Unit,
    onGotoArtistClick: (MPDSong) -> Unit,
) {
    val searchTerm by viewModel.songSearchTerm.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val searchResults by viewModel.songSearchResults.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .onPlaced { searchFocusRequester.requestFocus() },
                value = searchTerm,
                onValueChange = { viewModel.setSongSearchTerm(it) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.None,
                ),
                singleLine = true,
                label = { Text(stringResource(R.string.search_for_songs)) },
            )
        }

        if (searchTerm.length < 3) {
            Text(stringResource(R.string.enter_at_least_3_characters))
        } else {
            LazyColumn(state = listState) {
                items(searchResults) { song ->
                    val isCurrentSong = song.filename == currentSongFilename
                    val albumArt by viewModel.getAlbumArtState(song)

                    Divider()
                    SongRow(
                        modifier = if (isCurrentSong) Modifier.background(MaterialTheme.colorScheme.surfaceVariant) else Modifier,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        duration = song.duration,
                        year = song.year,
                        albumArt = albumArt,
                        isPlaying = isCurrentSong && playerState == PlayerState.PLAY,
                        onEnqueueClick = { viewModel.enqueueSong(song) },
                        onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                        highlight = searchTerm,
                        isCurrentSong = isCurrentSong,
                        onGotoAlbumClick = { onGotoAlbumClick(song) },
                        onGotoArtistClick = { onGotoArtistClick(song) },
                    )
                }
            }
        }
    }
}
