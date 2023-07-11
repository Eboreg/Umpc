package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.LibraryGrouping
import us.huseli.umpc.R
import us.huseli.umpc.compose.AlbumRow
import us.huseli.umpc.compose.ArtistRow
import us.huseli.umpc.compose.utils.ListWithAlphabetBar
import us.huseli.umpc.compose.utils.SubMenuScreen
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
) {
    val grouping by viewModel.grouping.collectAsStateWithLifecycle()
    val searchTerm by viewModel.librarySearchTerm.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val isSearchActive by viewModel.isLibrarySearchActive.collectAsStateWithLifecycle(false)

    SubMenuScreen(
        modifier = modifier,
        menu = {
            LibraryScreenSubMenu(
                grouping = grouping,
                isSearchActive = isSearchActive,
                setGrouping = { viewModel.setGrouping(it) },
                activateLibrarySearch = { viewModel.activateLibrarySearch(it) },
                deactivateLibrarySearch = { viewModel.deactivateLibrarySearch() },
            )
        }
    ) {
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
                        onClick = { viewModel.searchLibrary(grouping) },
                        content = { Icon(Icons.Sharp.Search, null) },
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.None,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.searchLibrary(grouping)
                        searchFocusRequester.freeFocus()
                    }
                ),
                singleLine = true,
            )
        }

        if (grouping == LibraryGrouping.ARTIST) {
            val artists by viewModel.artists.collectAsStateWithLifecycle()
            val artistLeadingChars by viewModel.artistLeadingChars.collectAsStateWithLifecycle(emptyList())

            ListWithAlphabetBar(
                modifier = Modifier.fillMaxWidth(),
                characters = artistLeadingChars,
                listState = viewModel.artistListState,
                items = artists,
                selector = { it.name },
            ) {
                LazyColumn(state = viewModel.artistListState, modifier = Modifier.fillMaxWidth()) {
                    items(artists, key = { it.name }) { artist ->
                        ArtistRow(
                            artist = artist,
                            padding = PaddingValues(start = 10.dp),
                            onGotoArtistClick = { onGotoArtistClick(artist.name) }
                        )
                        Divider()
                    }
                }
            }
        } else {
            val albums by viewModel.albums.collectAsStateWithLifecycle()
            val albumLeadingChars by viewModel.albumLeadingChars.collectAsStateWithLifecycle(emptyList())

            ListWithAlphabetBar(
                modifier = Modifier.fillMaxWidth(),
                characters = albumLeadingChars,
                listState = viewModel.albumListState,
                items = albums,
                selector = { it.name },
            ) {
                LazyColumn(state = viewModel.albumListState) {
                    items(albums, key = { it.hashCode() }) { album ->
                        LibraryScreenAlbumRow(
                            viewModel = viewModel,
                            album = album,
                            onGotoAlbumClick = onGotoAlbumClick,
                        )
                        Divider()
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
    onGotoAlbumClick: (MPDAlbum) -> Unit,
) {
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    var albumWithSongs by remember { mutableStateOf(MPDAlbumWithSongs(album, emptyList())) }

    LaunchedEffect(album) {
        viewModel.getAlbumWithSongsByAlbum(album) { albumWithSongs = it }
    }

    LaunchedEffect(albumWithSongs) {
        viewModel.getThumbnail(albumWithSongs) { thumbnail = it.thumbnail }
    }

    AlbumRow(
        album = albumWithSongs,
        thumbnail = thumbnail,
        showArtist = true,
        onGotoAlbumClick = { onGotoAlbumClick(album) },
    )
}
