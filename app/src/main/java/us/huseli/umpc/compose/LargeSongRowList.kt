package us.huseli.umpc.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.umpc.AddToPlaylistItemType
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.compose.utils.ListWithNumericBar
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.SongSelectViewModel
import kotlin.math.roundToInt

@Composable
fun LargeSongRowList(
    modifier: Modifier = Modifier,
    viewModel: SongSelectViewModel,
    songs: List<MPDSong>,
    listState: LazyListState,
    currentSong: MPDSong?,
    playerState: PlayerState?,
    highlight: String? = null,
    reorderable: Boolean = false,
    showSongPositions: Boolean = false,
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
    onAddSongToPlaylistClick: (MPDSong) -> Unit,
    onMoveSong: ((Int, Int) -> Unit)? = null,
    emptyListText: (@Composable () -> Unit)? = null,
    subMenu: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val selectedSongs by viewModel.selectedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.storedPlaylists.collectAsStateWithLifecycle()
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }

    // Somehow, this setup makes it so the list can both be manually reordered
    // _and_ get updated when it's changed externally. Don't really understand
    // how, though.
    val mutableSongs = remember { songs.toMutableStateList() }
    val reorderableState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to -> mutableSongs.add(to.index, mutableSongs.removeAt(from.index)) },
        onDragEnd = { from, to -> onMoveSong?.invoke(from, to) },
    )

    LaunchedEffect(songs) {
        snapshotFlow { songs }.distinctUntilChanged().collect {
            mutableSongs.clear()
            mutableSongs.addAll(it)
        }
    }

    if (isAddToPlaylistDialogOpen) {
        BatchAddToPlaylistDialog(
            itemCount = selectedSongs.size,
            itemType = AddToPlaylistItemType.SONG,
            playlists = playlists,
            addFunction = { playlistName, onFinish ->
                viewModel.addSelectedSongsToPlaylist(playlistName, onFinish)
            },
            addMessage = { viewModel.addMessage(it) },
            closeDialog = { isAddToPlaylistDialogOpen = false },
        )
    }

    Column(modifier = modifier) {
        subMenu?.invoke()

        if (selectedSongs.isNotEmpty()) {
            SelectedItemsSubMenu(
                pluralsResId = R.plurals.x_selected_songs,
                selectedItemCount = selectedSongs.size,
                onEnqueueClick = {
                    viewModel.enqueueSelectedSongs()
                    viewModel.addMessage(context.getString(R.string.enqueued_all_selected_songs))
                },
                onDeselectAllClick = { viewModel.deselectAllSongs() },
                onAddToPlaylistClick = { isAddToPlaylistDialogOpen = true },
            )
        }

        if (songs.isEmpty()) {
            emptyListText?.invoke()
        } else {
            ListWithNumericBar(
                modifier = modifier.fillMaxWidth(),
                listState = listState,
                listSize = songs.size,
                minItems = (LocalConfiguration.current.screenHeightDp * 0.028).roundToInt(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = if (reorderable) Modifier.reorderable(reorderableState) else Modifier,
                ) {
                    itemsIndexed(mutableSongs, key = { _, song -> song.id ?: song.filename }) { index, song ->
                        Divider()

                        ReorderableItem(reorderableState, key = song.id ?: song.filename) { isDragging ->
                            val rowModifier =
                                if (isDragging)
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, ShapeDefaults.ExtraSmall)
                                else Modifier
                            var albumArt by remember { mutableStateOf<ImageBitmap?>(null) }
                            var isExpanded by rememberSaveable { mutableStateOf(false) }

                            LaunchedEffect(song) {
                                viewModel.getAlbumArt(song) { albumArt = it.fullImage }
                            }

                            LargeSongRow(
                                modifier = rowModifier,
                                song = song,
                                isCurrentSong = currentSong == song,
                                isSelected = selectedSongs.contains(song),
                                isExpanded = isExpanded,
                                playerState = playerState,
                                onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                                onEnqueueClick = { viewModel.enqueueSong(song) },
                                onGotoAlbumClick = { onGotoAlbumClick(song.album) },
                                onGotoArtistClick = { onGotoArtistClick(song.artist) },
                                onAddToPlaylistClick = { onAddSongToPlaylistClick(song) },
                                artist = song.artist,
                                album = song.album.name,
                                albumArt = albumArt,
                                highlight = highlight,
                                position = if (showSongPositions) index + 1 else null,
                                onClick = {
                                    if (selectedSongs.isNotEmpty()) viewModel.toggleSongSelected(song)
                                    else isExpanded = !isExpanded
                                },
                                onLongClick = { viewModel.toggleSongSelected(song) },
                                albumArtModifier = if (reorderable) Modifier.detectReorder(reorderableState) else Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}
