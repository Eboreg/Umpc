package us.huseli.umpc.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.KeyboardDoubleArrowDown
import androidx.compose.material.icons.sharp.KeyboardDoubleArrowUp
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.viewmodels.QueueViewModel
import kotlin.math.max

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
    onGotoArtistClick: (String) -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentSongPosition by viewModel.currentSongPosition.collectAsStateWithLifecycle()
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Somehow, this setup makes it so the list can both be manually reordered
    // _and_ get updated when it's changed externally. Don't really understand
    // how, though.
    val localQueue = remember { queue.toMutableStateList() }
    val reorderableState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to -> localQueue.add(to.index, localQueue.removeAt(from.index)) },
        onDragEnd = { from, to -> viewModel.moveSong(from, to) },
    )

    LaunchedEffect(queue) {
        snapshotFlow { queue }.distinctUntilChanged().collect {
            localQueue.clear()
            localQueue.addAll(it)
        }
    }

    val scrollToTop: () -> Unit = {
        scope.launch { listState.scrollToItem(0) }
    }
    val scrollToCurrent: () -> Unit = {
        scope.launch { currentSongPosition?.let { listState.scrollToItem(max(0, it - 1)) } }
    }
    val scrollToBottom: () -> Unit = {
        scope.launch { listState.scrollToItem(queue.lastIndex) }
    }

    SubMenuScreen(
        modifier = modifier,
        menu = {
            SmallOutlinedButton(onClick = scrollToTop, leadingIcon = Icons.Sharp.KeyboardDoubleArrowUp) {
                Text(stringResource(R.string.top), modifier = Modifier.padding(start = 8.dp))
            }
            SmallOutlinedButton(onClick = scrollToCurrent, leadingIcon = Icons.Sharp.MusicNote) {
                Text(stringResource(R.string.now_playing), modifier = Modifier.padding(start = 8.dp))
            }
            SmallOutlinedButton(onClick = scrollToBottom, leadingIcon = Icons.Sharp.KeyboardDoubleArrowDown) {
                Text(stringResource(R.string.bottom), modifier = Modifier.padding(start = 8.dp))
            }
        }
    ) {
        ListWithScrollbar(
            modifier = Modifier.fillMaxWidth(),
            listSize = localQueue.size,
            listState = listState,
        ) {
            LazyColumn(modifier = Modifier.reorderable(reorderableState), state = listState) {
                items(localQueue, key = { it.id!! }) { song ->
                    ReorderableItem(reorderableState, key = song.id) { isDragging ->
                        val albumArt by viewModel.getAlbumArtState(song)
                        val rowModifier =
                            if (isDragging) Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, ShapeDefaults.ExtraSmall)
                            else Modifier

                        Divider()
                        SongRow(
                            modifier = rowModifier,
                            title = song.title,
                            isCurrentSong = song.id == currentSongId,
                            playerState = playerState,
                            onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                            onEnqueueClick = { viewModel.enqueueSong(song) },
                            onGotoAlbumClick = { onGotoAlbumClick(song.album) },
                            onGotoArtistClick = { onGotoArtistClick(song.artist) },
                            artist = song.artist,
                            album = song.album.name,
                            duration = song.duration,
                            year = song.year,
                            albumArt = albumArt,
                            showEnqueueButton = false,
                            albumArtModifier = Modifier.detectReorder(reorderableState),
                        )
                    }
                }
            }
        }
    }
}
