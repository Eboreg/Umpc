package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDSong) -> Unit,
    onGotoArtistClick: (MPDSong) -> Unit,
) {
    var grouping by rememberSaveable { mutableStateOf(LibraryGrouping.ARTIST) }
    val searchTerm by viewModel.librarySearchTerm.collectAsStateWithLifecycle()
    val searchFocusRequester = remember { FocusRequester() }
    val isSearchActive by viewModel.isLibrarySearchActive.collectAsStateWithLifecycle(false)
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilterChip(
                        shape = ShapeDefaults.ExtraSmall,
                        selected = grouping == LibraryGrouping.ARTIST,
                        onClick = { grouping = LibraryGrouping.ARTIST },
                        label = { Text(stringResource(R.string.group_by_artist)) },
                        leadingIcon = {
                            if (grouping == LibraryGrouping.ARTIST) Icon(Icons.Sharp.Done, null)
                        }
                    )
                    FilterChip(
                        shape = ShapeDefaults.ExtraSmall,
                        selected = grouping == LibraryGrouping.ALBUM,
                        onClick = { grouping = LibraryGrouping.ALBUM },
                        label = { Text(stringResource(R.string.group_by_album)) },
                        leadingIcon = {
                            if (grouping == LibraryGrouping.ALBUM) Icon(Icons.Sharp.Done, null)
                        }
                    )
                    IconToggleButton(
                        checked = isSearchActive,
                        onCheckedChange = {
                            if (it) viewModel.activateLibrarySearch(grouping)
                            else viewModel.deactivateLibrarySearch()
                        },
                        content = { Icon(Icons.Sharp.Search, stringResource(R.string.search)) },
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
            }
        }

        if (grouping == LibraryGrouping.ARTIST) {
            val artists by viewModel.artists.collectAsStateWithLifecycle()

            LazyColumn(state = listState) {
                items(artists, key = { it.name }) { artist ->
                    Divider()

                    ArtistSection(artist = artist) {
                        val albums = remember { mutableStateListOf<MPDAlbumWithSongs>() }

                        LaunchedEffect(artist) {
                            viewModel.getAlbumsWithSongsByAlbumArtist(artist.name) {
                                albums.addAll(it)
                            }
                        }

                        albums.forEach { album ->
                            var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                            LaunchedEffect(album) {
                                viewModel.getThumbnail(album) { thumbnail = it.thumbnail }
                            }

                            Divider()

                            AlbumRow(
                                album = album,
                                thumbnail = thumbnail,
                                onEnqueueClick = { viewModel.enqueueAlbum(album) },
                                onPlayClick = { viewModel.playAlbum(album) },
                            ) {
                                album.songs.forEach { song ->
                                    Divider()
                                    AlbumSongRow(
                                        song = song,
                                        album = album,
                                        currentSongFilename = currentSongFilename,
                                        playerState = playerState,
                                        onEnqueueClick = { viewModel.enqueueSong(song) },
                                        onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                                        onGotoAlbumClick = { onGotoAlbumClick(song) },
                                        onGotoArtistClick = { onGotoArtistClick(song) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val albums by viewModel.albums.collectAsStateWithLifecycle()

            LazyColumn(state = listState) {
                items(albums, key = { it.hashCode() }) { album ->
                    val albumWithSongs by viewModel.getAlbumWithSongs(album).collectAsStateWithLifecycle()
                    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(albumWithSongs) {
                        viewModel.getThumbnail(albumWithSongs) { thumbnail = it.thumbnail }
                    }

                    AlbumRow(
                        album = albumWithSongs,
                        showArtist = true,
                        thumbnail = thumbnail,
                        onEnqueueClick = { viewModel.enqueueAlbum(album) },
                        onPlayClick = { viewModel.playAlbum(album) },
                    ) {
                        albumWithSongs.songs.forEach { song ->
                            Divider()
                            AlbumSongRow(
                                song = song,
                                album = albumWithSongs,
                                currentSongFilename = currentSongFilename,
                                playerState = playerState,
                                onEnqueueClick = { viewModel.enqueueSong(song) },
                                onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                                onGotoAlbumClick = { onGotoAlbumClick(song) },
                                onGotoArtistClick = { onGotoArtistClick(song) },
                            )
                        }
                    }
                }
            }
        }
    }
}
