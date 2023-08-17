package us.huseli.umpc.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.PlaylistAdd
import androidx.compose.material.icons.sharp.PlaylistPlay
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.R
import us.huseli.umpc.compose.AddToPlaylistDialog
import us.huseli.umpc.compose.AlbumArt
import us.huseli.umpc.compose.BatchAddToPlaylistDialog
import us.huseli.umpc.compose.SelectedItemsSubMenu
import us.huseli.umpc.compose.SmallSongRow
import us.huseli.umpc.compose.utils.FadingImageBox
import us.huseli.umpc.compose.utils.SmallOutlinedButton
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.repository.SnackbarMessage
import us.huseli.umpc.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onGotoArtistClick: (String) -> Unit,
    onGotoPlaylistClick: (String) -> Unit,
    onAddSongToPlaylistClick: (MPDSong) -> Unit,
    onGotoQueueClick: () -> Unit,
) {
    val context = LocalContext.current
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    var isAddAlbumToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isAddSongsToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }
    val selectedSongs by viewModel.selectedSongs.collectAsStateWithLifecycle()

    val onEnqueueClick: () -> Unit = {
        viewModel.enqueueLast { response ->
            if (response.isSuccess) viewModel.addMessage(
                SnackbarMessage(
                    message = context.getString(R.string.the_album_was_enqueued),
                    actionLabel = context.getString(R.string.go_to_queue),
                    onActionPerformed = onGotoQueueClick,
                )
            )
            else viewModel.addError(
                context.resources.getQuantityString(
                    R.plurals.could_not_enqueue_albums,
                    1,
                    response.error ?: context.getString(R.string.unknown_error),
                )
            )
        }
    }

    if (isAddAlbumToPlaylistDialogOpen) {
        AddToPlaylistDialog(
            title = "\"${viewModel.album.name}\"",
            playlists = playlists,
            onConfirm = {
                viewModel.addToPlaylist(it) { response ->
                    if (response.isSuccess) viewModel.addMessage(
                        SnackbarMessage(
                            message = context.getString(R.string.album_was_added_to_playlist),
                            actionLabel = context.getString(R.string.go_to_playlist),
                            onActionPerformed = { onGotoPlaylistClick(it) },
                        )
                    )
                    else response.error?.let { error -> viewModel.addError(error) }
                }
                isAddAlbumToPlaylistDialogOpen = false
            },
            onCancel = { isAddAlbumToPlaylistDialogOpen = false },
        )
    }

    if (isAddSongsToPlaylistDialogOpen) {
        BatchAddToPlaylistDialog(
            itemCount = selectedSongs.size,
            itemType = AddToPlaylistItemType.SONG,
            playlists = playlists,
            addFunction = { playlistName, onFinish ->
                viewModel.addSelectedSongsToPlaylist(playlistName, onFinish)
            },
            addMessage = { viewModel.addMessage(it) },
            addError = { viewModel.addError(it) },
            closeDialog = { isAddSongsToPlaylistDialogOpen = false },
            onGotoPlaylistClick = onGotoPlaylistClick,
        )
    }

    if (selectedSongs.isNotEmpty()) {
        SelectedItemsSubMenu(
            pluralsResId = R.plurals.x_selected_songs,
            selectedItemCount = selectedSongs.size,
            onEnqueueClick = {
                viewModel.enqueueSelectedSongs { response ->
                    if (response.isSuccess) viewModel.addMessage(
                        SnackbarMessage(
                            message = context.getString(R.string.enqueued_all_selected_songs),
                            actionLabel = context.getString(R.string.go_to_queue),
                            onActionPerformed = onGotoQueueClick,
                        )
                    )
                    else viewModel.addError(
                        context.resources.getQuantityString(
                            R.plurals.could_not_enqueue_songs,
                            selectedSongs.size,
                            response.error ?: context.getString(R.string.unknown_error),
                        )
                    )
                }
            },
            onDeselectAllClick = { viewModel.deselectAllSongs() },
            onAddToPlaylistClick = { isAddSongsToPlaylistDialogOpen = true },
            onPlayClick = { viewModel.playSelectedSongs() },
        )
    }

    Column(modifier = modifier.verticalScroll(state = rememberScrollState())) {
        if (isInLandscapeMode()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(albumArt, forceSquare = true)
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    AlbumScreenMeta(
                        album = viewModel.album,
                        yearRange = albumWithSongs?.yearRange,
                        onGotoArtistClick = { onGotoArtistClick(viewModel.album.artist) },
                        onEnqueueClick = onEnqueueClick,
                        onPlayClick = { viewModel.play() },
                        onAddToPlaylistClick = { isAddAlbumToPlaylistDialogOpen = true },
                    )
                }
            }
        } else {
            FadingImageBox(
                modifier = Modifier.fillMaxWidth(),
                image = { AlbumArt(imageBitmap = albumArt) },
                topContent = {},
                bottomContent = {
                    AlbumScreenMeta(
                        album = viewModel.album,
                        yearRange = albumWithSongs?.yearRange,
                        onGotoArtistClick = { onGotoArtistClick(viewModel.album.artist) },
                        onEnqueueClick = onEnqueueClick,
                        onPlayClick = { viewModel.play() },
                        onAddToPlaylistClick = { isAddAlbumToPlaylistDialogOpen = true },
                    )
                }
            )
        }

        albumWithSongs?.let { album ->
            val discNumbers = album.songs.mapNotNull { it.discNumber }.toSet()

            album.songs.forEachIndexed { index, song ->
                var isExpanded by rememberSaveable { mutableStateOf(false) }

                SmallSongRow(
                    song = song,
                    discNumber = if (discNumbers.size > 1) song.discNumber else null,
                    isCurrentSong = currentSongFilename == song.filename,
                    isExpanded = isExpanded,
                    isSelected = selectedSongs.contains(song),
                    playerState = playerState,
                    showYear = albumWithSongs?.yearRange?.first != albumWithSongs?.yearRange?.last,
                    onEnqueueClick = {
                        viewModel.enqueueSongLast(song) { response ->
                            if (response.isSuccess) viewModel.addMessage(
                                SnackbarMessage(
                                    message = context.getString(R.string.the_song_was_enqueued),
                                    actionLabel = context.getString(R.string.go_to_queue),
                                    onActionPerformed = onGotoQueueClick,
                                )
                            )
                            else viewModel.addError(
                                context.resources.getQuantityString(
                                    R.plurals.could_not_enqueue_songs,
                                    1,
                                    response.error ?: context.getString(R.string.unknown_error),
                                )
                            )
                        }
                    },
                    onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                    onGotoArtistClick = { onGotoArtistClick(song.artist) },
                    onAddToPlaylistClick = { onAddSongToPlaylistClick(song) },
                    onClick = {
                        if (selectedSongs.isNotEmpty()) viewModel.toggleSongSelected(song)
                        else isExpanded = !isExpanded
                    },
                    onLongClick = { viewModel.toggleSongSelected(song) },
                )
                if (index < album.songs.size - 1) Divider()
            }
        }
    }
}

@Composable
fun AlbumScreenMeta(
    album: MPDAlbum,
    yearRange: IntRange? = null,
    onGotoArtistClick: () -> Unit,
    onEnqueueClick: () -> Unit,
    onPlayClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
) {
    Text(
        text = album.name,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )
    if (yearRange != null) {
        Text(
            text = if (yearRange.first != yearRange.last) "${yearRange.first} - ${yearRange.last}" else yearRange.first.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
    Text(
        text = album.artist,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable { onGotoArtistClick() },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        SmallOutlinedButton(
            onClick = onEnqueueClick,
            leadingIcon = Icons.Sharp.PlaylistPlay,
            text = stringResource(R.string.enqueue),
        )
        SmallOutlinedButton(
            onClick = onPlayClick,
            leadingIcon = Icons.Sharp.PlayArrow,
            text = stringResource(R.string.play),
        )
        SmallOutlinedButton(
            onClick = onAddToPlaylistClick,
            leadingIcon = Icons.Sharp.PlaylistAdd,
            text = stringResource(R.string.add_to_playlist),
        )
    }
}
