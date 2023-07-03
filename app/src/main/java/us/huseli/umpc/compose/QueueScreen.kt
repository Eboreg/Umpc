package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.KeyboardDoubleArrowDown
import androidx.compose.material.icons.sharp.KeyboardDoubleArrowUp
import androidx.compose.material.icons.sharp.MusicNote
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import us.huseli.umpc.PlayerState
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.QueueViewModel
import kotlin.math.max

@Composable
fun QueueScreen(
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onGotoAlbumClick: (MPDSong) -> Unit,
    onGotoArtistClick: (MPDSong) -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentSongIndex by viewModel.currentSongIndex.collectAsStateWithLifecycle()
    val currentSongId by viewModel.currentSongId.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val scrollToTop: () -> Unit = {
        scope.launch { listState.scrollToItem(0) }
    }
    val scrollToCurrent: () -> Unit = {
        scope.launch { currentSongIndex?.let { listState.scrollToItem(max(0, it - 1)) } }
    }
    val scrollToBottom: () -> Unit = {
        scope.launch { listState.scrollToItem(queue.lastIndex) }
    }

    Column(modifier = modifier) {
        Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = scrollToTop,
                    shape = ShapeDefaults.ExtraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Sharp.KeyboardDoubleArrowUp, null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.top))
                }
                OutlinedButton(
                    onClick = scrollToCurrent,
                    shape = ShapeDefaults.ExtraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Sharp.MusicNote, null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.now_playing))
                }
                OutlinedButton(
                    onClick = scrollToBottom,
                    shape = ShapeDefaults.ExtraSmall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Sharp.KeyboardDoubleArrowDown, null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.bottom))
                }
            }
        }

        LazyColumn(modifier = modifier, state = listState) {
            items(queue) { song ->
                val isCurrentSong = song.id == currentSongId
                val albumArt by viewModel.getAlbumArtState(song)

                Divider()

                SongRow(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration,
                    albumArt = albumArt,
                    isPlaying = isCurrentSong && playerState == PlayerState.PLAY,
                    isCurrentSong = isCurrentSong,
                    onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                    onEnqueueClick = { viewModel.enqueueSong(song) },
                    onGotoAlbumClick = { onGotoAlbumClick(song) },
                    onGotoArtistClick = { onGotoArtistClick(song) },
                )
            }
        }
    }
}
