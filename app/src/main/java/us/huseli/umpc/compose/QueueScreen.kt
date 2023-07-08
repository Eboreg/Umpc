package us.huseli.umpc.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import us.huseli.umpc.formatDuration
import us.huseli.umpc.viewmodels.QueueViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
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
    var totalDuration by rememberSaveable { mutableStateOf(0.0) }

    // Somehow, this setup makes it so the list can both be manually reordered
    // _and_ get updated when it's changed externally. Don't really understand
    // how, though.
    val localQueue = remember { queue.toMutableStateList() }
    val reorderableState = rememberReorderableLazyListState(
        listState = listState,
        onMove = { from, to -> localQueue.add(to.index, localQueue.removeAt(from.index)) },
        onDragEnd = { from, to -> viewModel.moveSong(from, to) },
    )

    val scrollToCurrent: () -> Unit = {
        scope.launch { currentSongPosition?.let { listState.scrollToItem(max(0, it - 1)) } }
    }

    LaunchedEffect(queue) {
        scrollToCurrent()
        totalDuration = queue.mapNotNull { it.duration }.sum()
        snapshotFlow { queue }.distinctUntilChanged().collect {
            localQueue.clear()
            localQueue.addAll(it)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Badge { Text(pluralStringResource(R.plurals.x_songs, localQueue.size, localQueue.size)) }
                Badge { Text(totalDuration.formatDuration()) }
            }
        }

        ListWithScrollbar(
            modifier = modifier.fillMaxWidth(),
            listSize = localQueue.size,
            listState = listState,
        ) {
            LazyColumn(modifier = Modifier.reorderable(reorderableState), state = listState) {
                itemsIndexed(localQueue, key = { _, song -> song.id!! }) { index, song ->
                    Divider()

                    ReorderableItem(reorderableState, key = song.id) { isDragging ->
                        val albumArt by viewModel.getAlbumArtState(song)
                        val rowModifier =
                            if (isDragging) Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, ShapeDefaults.ExtraSmall)
                            else Modifier

                        LargeSongRow(
                            modifier = rowModifier,
                            song = song,
                            isCurrentSong = song.id == currentSongId,
                            playerState = playerState,
                            onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                            onEnqueueClick = { viewModel.enqueueSong(song) },
                            onGotoAlbumClick = { onGotoAlbumClick(song.album) },
                            onGotoArtistClick = { onGotoArtistClick(song.artist) },
                            artist = song.artist,
                            album = song.album.name,
                            albumArt = albumArt,
                            showEnqueueButton = false,
                            albumArtModifier = Modifier.detectReorder(reorderableState),
                            position = index + 1,
                        )
                    }
                }
            }
        }
    }
}
