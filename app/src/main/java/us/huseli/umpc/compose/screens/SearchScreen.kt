package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.R
import us.huseli.umpc.compose.BatchAddToPlaylistDialog
import us.huseli.umpc.compose.LargeSongRowList
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
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    val enqueuingAllMessage = stringResource(R.string.enqueuing_all_search_results)

    if (isAddToPlaylistDialogOpen) {
        BatchAddToPlaylistDialog(
            itemCount = searchResults.size,
            itemType = AddToPlaylistItemType.SONG,
            playlists = playlists,
            addFunction = { playlistName, onFinish ->
                viewModel.addAllToPlaylist(playlistName, onFinish)
            },
            addMessage = { viewModel.addMessage(it) },
            closeDialog = { isAddToPlaylistDialogOpen = false },
        )
    }

    LargeSongRowList(
        modifier = modifier,
        viewModel = viewModel,
        songs = if (searchTerm.text.length < 3) emptyList() else searchResults,
        listState = viewModel.listState,
        currentSong = currentSong,
        playerState = playerState,
        highlight = searchTerm.text,
        onGotoAlbumClick = onGotoAlbumClick,
        onGotoArtistClick = onGotoArtistClick,
        onAddSongToPlaylistClick = onAddSongToPlaylistClick,
        emptyListText = {
            Text(
                text = stringResource(R.string.enter_at_least_3_characters),
                modifier = Modifier.padding(10.dp)
            )
        },
        subMenu = {
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
        }
    )
}
