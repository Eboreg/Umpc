package us.huseli.umpc.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.AlbumViewModel

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onGotoArtistClick: (MPDSong) -> Unit,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val album by viewModel.album.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Column(modifier = modifier.verticalScroll(state = rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (albumArt != null) {
                val brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                    startY = 0.75f
                )
                AlbumArt(imageBitmap = albumArt, modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.matchParentSize().background(brush = brush))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AutoScrollingTextLine(
                    text = album?.name ?: "-",
                    style = MaterialTheme.typography.titleLarge,
                )
                AutoScrollingTextLine(
                    text = album?.artist ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        modifier = Modifier.height(35.dp),
                        onClick = { viewModel.enqueueAlbum(album) },
                        contentPadding = PaddingValues(8.dp, 0.dp),
                        shape = ShapeDefaults.ExtraSmall,
                    ) {
                        Text(stringResource(R.string.enqueue), modifier = Modifier.padding(end = 4.dp))
                        Icon(Icons.Sharp.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    OutlinedButton(
                        modifier = Modifier.height(35.dp),
                        onClick = { viewModel.playAlbum(album) },
                        contentPadding = PaddingValues(10.dp, 0.dp),
                        shape = ShapeDefaults.ExtraSmall,
                    ) {
                        Text(stringResource(R.string.play), modifier = Modifier.padding(end = 4.dp))
                        Icon(Icons.Sharp.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        album?.songs?.forEach { song ->
            Divider()
            AlbumSongRow(
                song = song,
                album = album,
                currentSongFilename = currentSongFilename,
                playerState = playerState,
                onEnqueueClick = { viewModel.enqueueSong(song) },
                onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                onGotoArtistClick = { onGotoArtistClick(song) },
            )
        }
    }
}
