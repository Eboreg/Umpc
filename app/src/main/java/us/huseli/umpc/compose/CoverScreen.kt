package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.viewmodels.CoverViewModel

@Composable
fun CoverScreen(
    modifier: Modifier = Modifier,
    viewModel: CoverViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDSong) -> Unit,
    onGotoArtistClick: (MPDSong) -> Unit,
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentSongAlbumArt by viewModel.currentSongAlbumArt.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val currentSongElapsed by viewModel.currentSongElapsed.collectAsStateWithLifecycle()
    val currentSongDuration by viewModel.currentSongDuration.collectAsStateWithLifecycle()
    val repeatState by viewModel.repeatState.collectAsStateWithLifecycle()
    val randomState by viewModel.randomState.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        VolumeSlider(
            value = volume.toFloat(),
            onValueChange = { viewModel.setVolume(it.toInt()) },
        )

        FadingImageBox(
            modifier = Modifier.fillMaxWidth(),
            image = { AlbumArt(imageBitmap = currentSongAlbumArt?.fullImage, modifier = Modifier.fillMaxWidth()) }
        ) {
            currentSong?.let { song ->
                AutoScrollingTextLine(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                AutoScrollingTextLine(
                    modifier = Modifier.clickable { onGotoArtistClick(song) },
                    text = song.artist,
                    style = MaterialTheme.typography.titleLarge,
                )
                AutoScrollingTextLine(
                    modifier = Modifier.clickable { onGotoAlbumClick(song) },
                    text = song.album,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        SongProgressSlider(
            elapsed = currentSongElapsed ?: 0.0,
            duration = currentSongDuration ?: 0.0,
            playerState = playerState,
            onManualChange = { viewModel.seek(it) },
        )

        PlayerControls(
            modifier = Modifier.fillMaxWidth(),
            buttonHeight = 50.dp,
            playerState = playerState,
            repeatState = repeatState,
            randomState = randomState,
            onPreviousClick = { viewModel.previous() },
            onPlayPauseClick = { viewModel.playOrPause() },
            onStopClick = { viewModel.stop() },
            onNextClick = { viewModel.next() },
            onRandomClick = { viewModel.toggleRandomState() },
            onRepeatClick = { viewModel.toggleRepeatState() },
        )
    }
}
