package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.R
import us.huseli.umpc.compose.AlbumArtGrid
import us.huseli.umpc.compose.AlbumRow
import us.huseli.umpc.compose.BatchAddToPlaylistDialog
import us.huseli.umpc.compose.SelectedItemsSubMenu
import us.huseli.umpc.compose.utils.FadingImageBox
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.ArtistViewModel

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
    onGotoQueueClick: () -> Unit,
) {
    val context = LocalContext.current
    val albums by viewModel.albums.collectAsStateWithLifecycle(emptyList())
    val appearsOnAlbums by viewModel.appearsOnAlbums.collectAsStateWithLifecycle(emptyList())
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val totalDuration by viewModel.totalDuration.collectAsStateWithLifecycle()
    val selectedAlbums by viewModel.selectedAlbums.collectAsStateWithLifecycle()
    var isAddAlbumsToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    val landscape = isInLandscapeMode()
    val gridAlbumArt by viewModel.gridAlbumArt.collectAsStateWithLifecycle()

    if (isAddAlbumsToPlaylistDialogOpen) {
        BatchAddToPlaylistDialog(
            itemType = AddToPlaylistItemType.ALBUM,
            itemCount = selectedAlbums.size,
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

    if (selectedAlbums.isNotEmpty()) {
        SelectedItemsSubMenu(
            pluralsResId = R.plurals.x_selected_albums,
            selectedItemCount = selectedAlbums.size,
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
            onDeselectAllClick = { viewModel.deselectAllAlbums() },
            onAddToPlaylistClick = { isAddAlbumsToPlaylistDialogOpen = true },
        )
    }

    LazyColumn(modifier = modifier, state = viewModel.listState) {
        if (landscape) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArtGrid(
                        albumArtList = gridAlbumArt,
                        modifier = Modifier.width(140.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxHeight(),
                        content = {
                            Text(
                                text = viewModel.artist,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            ArtistScreenMeta(albums.size, songCount, totalDuration)
                        },
                    )
                }
            }
        } else {
            item {
                FadingImageBox(
                    modifier = Modifier.fillMaxWidth(),
                    image = { AlbumArtGrid(albumArtList = gridAlbumArt) },
                    topContent = {},
                    bottomContent = {
                        Text(
                            text = viewModel.artist,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            content = { ArtistScreenMeta(albums.size, songCount, totalDuration) },
                        )
                    },
                )
            }
        }

        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.albums),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(10.dp),
                )
            }
            itemsIndexed(albums) { index, album ->
                var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(album) {
                    viewModel.getThumbnail(album) { thumbnail = it.thumbnail }
                }

                AlbumRow(
                    album = album,
                    thumbnail = thumbnail,
                    showArtist = album.album.artist != viewModel.artist,
                    isSelected = selectedAlbums.contains(album.album),
                    onClick = {
                        if (selectedAlbums.isNotEmpty()) viewModel.toggleAlbumSelected(album.album)
                        else onGotoAlbumClick(album.album)
                    },
                    onLongClick = { viewModel.toggleAlbumSelected(album.album) },
                )
                if (index < albums.size - 1) Divider()
            }
        }

        if (appearsOnAlbums.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.appears_on),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(10.dp),
                )
            }
            itemsIndexed(appearsOnAlbums) { index, album ->
                var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(album) {
                    viewModel.getThumbnail(album) { thumbnail = it.thumbnail }
                }

                AlbumRow(
                    album = album,
                    thumbnail = thumbnail,
                    showArtist = album.album.artist != viewModel.artist,
                    isSelected = selectedAlbums.contains(album.album),
                    onClick = {
                        if (selectedAlbums.isNotEmpty()) viewModel.toggleAlbumSelected(album.album)
                        else onGotoAlbumClick(album.album)
                    },
                    onLongClick = { viewModel.toggleAlbumSelected(album.album) },
                )
                if (index < appearsOnAlbums.size - 1) Divider()
            }
        }
    }
}

@Composable
fun ArtistScreenMeta(albumCount: Int, songCount: Int, totalDuration: Double) {
    Text(
        pluralStringResource(R.plurals.x_albums, albumCount, albumCount),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        pluralStringResource(R.plurals.x_songs, songCount, songCount),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        stringResource(R.string.total_time_colon, totalDuration.formatDuration()),
        style = MaterialTheme.typography.bodySmall,
    )
}
