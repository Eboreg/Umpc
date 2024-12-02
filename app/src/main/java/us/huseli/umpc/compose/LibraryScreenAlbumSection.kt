package us.huseli.umpc.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import us.huseli.retaintheme.compose.ListWithAlphabetBar
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.replaceLeadingJunk
import us.huseli.umpc.viewmodels.LibraryViewModel
import kotlin.math.roundToInt

@Composable
fun LibraryScreenAlbumSection(
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val albumLeadingChars by viewModel.albumLeadingChars.collectAsStateWithLifecycle(emptyList())
    val albumsLoaded by viewModel.albumsLoaded.collectAsStateWithLifecycle()
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .distinctUntilChanged()
            .collect { visibleItems ->
                @Suppress("UNCHECKED_CAST")
                viewModel.loadAlbumsWithSongs(visibleItems.map { it.key } as List<MPDAlbum>)
            }
    }

    if (albums.isEmpty()) {
        Text(
            text = stringResource(if (!albumsLoaded) R.string.loading else R.string.no_albums_found_you_may_want_to_add),
            modifier = Modifier.padding(10.dp),
        )
    }

    ListWithAlphabetBar(
        modifier = modifier.fillMaxWidth(),
        characters = albumLeadingChars,
        listState = listState,
        items = albums,
        selector = { it.name.replaceLeadingJunk() },
        minItems = (LocalConfiguration.current.screenHeightDp * 0.042).roundToInt(),
    ) {
        LazyColumn(state = listState) {
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