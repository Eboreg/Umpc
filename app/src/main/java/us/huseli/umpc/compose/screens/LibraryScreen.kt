package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Done
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.R
import us.huseli.umpc.compose.AlbumRow
import us.huseli.umpc.compose.ArtistRow
import us.huseli.umpc.compose.BatchAddToPlaylistDialog
import us.huseli.umpc.compose.NotConnectedToMPD
import us.huseli.umpc.compose.SelectedItemsSubMenu
import us.huseli.umpc.compose.utils.ListWithAlphabetBar
import us.huseli.umpc.compose.utils.SubMenuScreen
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.replaceLeadingJunk
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.LibraryViewModel
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    albumListState: LazyListState = rememberLazyListState(),
    artistListState: LazyListState = rememberLazyListState(),
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
                val artists by viewModel.artists.collectAsStateWithLifecycle()
                val artistLeadingChars by viewModel.artistLeadingChars.collectAsStateWithLifecycle(emptyList())

                if (artists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_artists_found_you_may_want_to_add),
                        modifier = Modifier.padding(10.dp),
                    )
                }

                ListWithAlphabetBar(
                    modifier = Modifier.fillMaxWidth(),
                    characters = artistLeadingChars,
                    listState = artistListState,
                    items = artists,
                    selector = { it.name.replaceLeadingJunk() },
                    minItems = (LocalConfiguration.current.screenHeightDp * 0.042).roundToInt(),
                ) {
                    LazyColumn(state = artistListState, modifier = Modifier.fillMaxWidth()) {
                        items(artists, key = { it.name }) { artist ->
                            ArtistRow(
                                artist = artist,
                                padding = PaddingValues(10.dp),
                                onGotoArtistClick = { onGotoArtistClick(artist.name) }
                            )
                            Divider()
                        }
                    }
                }
            }
            LibraryGrouping.ALBUM -> {
                val albums by viewModel.albums.collectAsStateWithLifecycle()
                val albumLeadingChars by viewModel.albumLeadingChars.collectAsStateWithLifecycle(emptyList())

                LaunchedEffect(albumListState) {
                    snapshotFlow { albumListState.layoutInfo.visibleItemsInfo }
                        .distinctUntilChanged()
                        .collect { visibleItems ->
                            @Suppress("UNCHECKED_CAST")
                            viewModel.loadAlbumsWithSongs(visibleItems.map { it.key } as List<MPDAlbum>)
                        }
                }

                if (albums.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_albums_found_you_may_want_to_add),
                        modifier = Modifier.padding(10.dp),
                    )
                }

                ListWithAlphabetBar(
                    modifier = Modifier.fillMaxWidth(),
                    characters = albumLeadingChars,
                    listState = albumListState,
                    items = albums,
                    selector = { it.name.replaceLeadingJunk() },
                    minItems = (LocalConfiguration.current.screenHeightDp * 0.042).roundToInt(),
                ) {
                    LazyColumn(state = albumListState) {
                        items(albums, key = { it }) { album ->
                            LibraryScreenAlbumRow(
                                viewModel = viewModel,
                                album = album,
                                isSelected = selectedAlbums.contains(album),
                                onClick = {
                                    if (selectedAlbums.isNotEmpty()) viewModel.toggleAlbumSelected(album)
                                    else onGotoAlbumClick(album)
                                },
                                onLongClick = { viewModel.toggleAlbumSelected(album) },
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenSubMenu(
    grouping: LibraryGrouping,
    isSearchActive: Boolean,
    isConnected: Boolean,
    setGrouping: (LibraryGrouping) -> Unit,
    activateLibrarySearch: (LibraryGrouping) -> Unit,
    deactivateLibrarySearch: () -> Unit,
) {
    FilterChip(
        shape = ShapeDefaults.ExtraSmall,
        selected = grouping == LibraryGrouping.ARTIST,
        onClick = { setGrouping(LibraryGrouping.ARTIST) },
        label = { Text(stringResource(R.string.group_by_artist)) },
        leadingIcon = {
            if (grouping == LibraryGrouping.ARTIST) Icon(Icons.Sharp.Done, null)
        }
    )
    FilterChip(
        shape = ShapeDefaults.ExtraSmall,
        selected = grouping == LibraryGrouping.ALBUM,
        onClick = { setGrouping(LibraryGrouping.ALBUM) },
        label = { Text(stringResource(R.string.group_by_album)) },
        leadingIcon = {
            if (grouping == LibraryGrouping.ALBUM) Icon(Icons.Sharp.Done, null)
        }
    )
    IconToggleButton(
        checked = isSearchActive,
        enabled = isConnected,
        onCheckedChange = {
            if (it) activateLibrarySearch(grouping)
            else deactivateLibrarySearch()
        },
        content = { Icon(Icons.Sharp.Search, stringResource(R.string.search)) },
    )
}

@Composable
fun LibraryScreenAlbumRow(
    viewModel: LibraryViewModel,
    album: MPDAlbum,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    val albumWithSongs by viewModel.getAlbumWithSongsFlow(album)
        .collectAsStateWithLifecycle(MPDAlbumWithSongs(album, emptyList()))

    LaunchedEffect(albumWithSongs) {
        viewModel.getAlbumArt(albumWithSongs) { thumbnail = it.thumbnail }
    }

    AlbumRow(
        album = albumWithSongs,
        thumbnail = thumbnail,
        showArtist = true,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}
