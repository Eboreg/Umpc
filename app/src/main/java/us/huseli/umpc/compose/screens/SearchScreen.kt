package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.compose.AddToPlaylistDialog
import us.huseli.umpc.compose.LargeSongRow
import us.huseli.umpc.compose.utils.SmallOutlinedButton
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.SearchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onAddSongToPlaylistClick: (MPDSong) -> Unit,
) {
    val searchTerm by viewModel.songSearchTerm.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val searchResults by viewModel.songSearchResults.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    val enqueuingAllMessage = stringResource(R.string.enqueuing_all_search_results)
    val addingAllMessage = stringResource(R.string.adding_all_search_results_to_playlist)

    if (isAddToPlaylistDialogOpen) {
        AddToPlaylistDialog(
            title = pluralStringResource(R.plurals.x_songs, searchResults.size, searchResults.size),
            playlists = playlists,
            onConfirm = {
                viewModel.addMessage(addingAllMessage)
                viewModel.addAllToPlaylist(it)
                isAddToPlaylistDialogOpen = false
            },
            onCancel = { isAddToPlaylistDialogOpen = false },
        )
    }

    Column(modifier = modifier) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                FlowRow(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val resultCount =
                        if (searchTerm.text.length < 3) 0
                        else searchResults.size
                    Text(
                        text = pluralStringResource(R.plurals.x_songs_found, resultCount, resultCount),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (resultCount > 0) {
                        SmallOutlinedButton(
                            onClick = {
                                viewModel.addMessage(enqueuingAllMessage)
                                viewModel.enqueueAll()
                            },
                            leadingIcon = Icons.Sharp.PlaylistPlay,
                            text = stringResource(R.string.enqueue_all),
                        )
                        SmallOutlinedButton(
                            onClick = { isAddToPlaylistDialogOpen = true },
                            leadingIcon = Icons.Sharp.PlaylistAdd,
                            text = stringResource(R.string.add_all_to_playlist),
                        )
                    }
                }
            }
        }

        if (searchTerm.text.length < 3) {
            Text(stringResource(R.string.enter_at_least_3_characters), modifier = Modifier.padding(10.dp))
        } else {
            LazyColumn(state = viewModel.listState) {
                items(searchResults) { song ->
                    var albumArt by remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(song) {
                        viewModel.getAlbumArt(song) { albumArt = it.fullImage }
                    }

                    Divider()
                    LargeSongRow(
                        song = song,
                        isCurrentSong = song.filename == currentSongFilename,
                        playerState = playerState,
                        onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                        onEnqueueClick = { viewModel.enqueueSong(song) },
                        onGotoAlbumClick = { onGotoAlbumClick(song.album) },
                        onGotoArtistClick = { onGotoArtistClick(song.artist) },
                        onAddToPlaylistClick = { onAddSongToPlaylistClick(song) },
                        artist = song.artist,
                        album = song.album.name,
                        albumArt = albumArt,
                        highlight = searchTerm.text,
                    )
                }
            }
        }
    }
}
