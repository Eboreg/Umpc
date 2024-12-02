package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.R
import us.huseli.umpc.compose.BatchAddToPlaylistDialog
import us.huseli.umpc.compose.LibraryScreenAlbumSection
import us.huseli.umpc.compose.LibraryScreenArtistSection
import us.huseli.umpc.compose.LibraryScreenDirectorySection
import us.huseli.umpc.compose.LibraryScreenSubMenu
import us.huseli.umpc.compose.NotConnectedToMPD
import us.huseli.umpc.compose.SelectedItemsSubMenu
import us.huseli.umpc.compose.utils.SubMenuScreen
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    albumListState: LazyListState = rememberLazyListState(),
    artistListState: LazyListState = rememberLazyListState(),
    directoryListState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
    onGotoQueueClick: () -> Unit,
) {
    val context = LocalContext.current
    val grouping by viewModel.grouping.collectAsStateWithLifecycle()
    val searchTerm by viewModel.librarySearchTerm.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val isSearchActive by viewModel.isLibrarySearchActive.collectAsStateWithLifecycle(false)
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
    var isAddAlbumsToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    if (isAddAlbumsToPlaylistDialogOpen) {
        BatchAddToPlaylistDialog(
            itemCount = selectedAlbums.size,
            itemType = AddToPlaylistItemType.ALBUM,
            playlists = playlists,
            addFunction = { playlistName, onFinish ->
                viewModel.addSelectedAlbumsToPlaylist(playlistName, onFinish)
            },
            addMessage = { viewModel.addMessage(it) },
            addError = { viewModel.addError(it) },
            closeDialog = { isAddAlbumsToPlaylistDialogOpen = false },
            onGotoPlaylistClick = onGotoPlaylistClick,
        )
    }

    SubMenuScreen(
        modifier = modifier,
        menu = {
            LibraryScreenSubMenu(
                grouping = grouping,
                isSearchActive = isSearchActive,
                setGrouping = { viewModel.setGrouping(it) },
                activateLibrarySearch = { viewModel.activateLibrarySearch(it) },
                deactivateLibrarySearch = { viewModel.deactivateLibrarySearch() },
                isConnected = isConnected,
            )
        }
    ) {
        if (!isConnected) NotConnectedToMPD()

        if (selectedAlbums.isNotEmpty()) {
            SelectedItemsSubMenu(
                selectedItemCount = selectedAlbums.size,
                pluralsResId = R.plurals.x_selected_albums,
                onDeselectAllClick = { viewModel.deselectAllAlbums() },
                isConnected = isConnected,
                onEnqueueClick = {
                    viewModel.enqueueSelectedAlbums { response ->
                        if (response.isSuccess) viewModel.addMessage(
                            SnackbarMessage(
                                message = context.getString(R.string.enqueued_all_selected_albums),
                                actionLabel = context.getString(R.string.go_to_queue),
                                onActionPerformed = onGotoQueueClick,
                            )
                        )
                        else viewModel.addError(
                            context.resources.getQuantityString(
                                R.plurals.could_not_enqueue_albums,
                                viewModel.selectedAlbums.value.size,
                                response.error ?: context.getString(R.string.unknown_error),
                            )
                        )
                    }
                },
                onAddToPlaylistClick = { isAddAlbumsToPlaylistDialogOpen = true },
                onPlayClick = { viewModel.playSelectedAlbums() },
            )
        }

        if (isSearchActive) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .onPlaced { searchFocusRequester.requestFocus() },
                value = searchTerm,
                onValueChange = { viewModel.setLibrarySearchTerm(it) },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.searchLibrary() },
                        content = { Icon(Icons.Sharp.Search, null) },
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.None,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.searchLibrary()
                        searchFocusRequester.freeFocus()
                    }
                ),
                singleLine = true,
            )
        }

        when (grouping) {
            LibraryGrouping.ARTIST -> {
                LibraryScreenArtistSection(
                    onGotoArtistClick = onGotoArtistClick,
                    viewModel = viewModel,
                    listState = artistListState,
                )
            }

            LibraryGrouping.ALBUM -> {
                LibraryScreenAlbumSection(
                    onGotoAlbumClick = onGotoAlbumClick,
                    viewModel = viewModel,
                    listState = albumListState,
                )
            }
            LibraryGrouping.DIRECTORY -> {
                LibraryScreenDirectorySection(listState = directoryListState)
            }
        }
    }
}
