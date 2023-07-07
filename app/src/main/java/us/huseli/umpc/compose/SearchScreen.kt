package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.viewmodels.SearchViewModel

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
) {
    val searchTerm by viewModel.songSearchTerm.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val searchResults by viewModel.songSearchResults.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
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
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp).size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            )
        }

        if (searchTerm.text.length < 3) {
            Text(stringResource(R.string.enter_at_least_3_characters), modifier = Modifier.padding(10.dp))
        } else if (searchResults.isEmpty()) {
            Text(stringResource(R.string.no_songs_were_found), modifier = Modifier.padding(10.dp))
        } else {
            LazyColumn(state = listState) {
                items(searchResults) { song ->
                    val albumArt by viewModel.getAlbumArtState(song)

                    Divider()
                    SongRow(
                        title = song.title,
                        isCurrentSong = song.filename == currentSongFilename,
                        playerState = playerState,
                        onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                        onEnqueueClick = { viewModel.enqueueSong(song) },
                        onGotoAlbumClick = { onGotoAlbumClick(song.album) },
                        onGotoArtistClick = { onGotoArtistClick(song.artist) },
                        artist = song.artist,
                        album = song.album.name,
                        duration = song.duration,
                        year = song.year,
                        albumArt = albumArt,
                        highlight = searchTerm.text,
                    )
                }
            }
        }
    }
}
