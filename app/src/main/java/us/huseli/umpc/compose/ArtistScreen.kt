package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.formatDuration
import us.huseli.umpc.viewmodels.ArtistViewModel

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDSong) -> Unit,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val albumArtMap = remember { mutableStateMapOf<String, MPDAlbumArt>() }
    val songCount = albums.sumOf { it.songs.size }
    val totalDuration = albums.mapNotNull { it.duration }.sum()

    LaunchedEffect(albums) {
        albumArtMap.clear()
        viewModel.getAlbumArt(
            keys = albums.map { it.albumArtKey },
            callback = { albumArtMap[it.key.album] = it }
        )
    }

    Column(modifier = modifier.verticalScroll(state = rememberScrollState())) {
        FadingImageBox(
            modifier = Modifier.fillMaxWidth(),
            image = { AlbumArtGrid(albumArtList = albumArtMap.map { it.value }) },
        ) {
            Text(
                text = viewModel.artist,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    pluralStringResource(R.plurals.x_albums, albums.size, albums.size),
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
        }

        albums.forEach { album ->
            AlbumRow(
                album = album,
                showArtist = album.artist != viewModel.artist,
                thumbnail = albumArtMap[album.name]?.thumbnail,
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
                    )
                }
            }
        }
    }
}
