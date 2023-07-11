package us.huseli.umpc.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.compose.AlbumArtGrid
import us.huseli.umpc.compose.AlbumRow
import us.huseli.umpc.compose.utils.FadingImageBox
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.ArtistViewModel

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

@Composable
fun ArtistScreen(
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
    onGotoAlbumClick: (MPDAlbum) -> Unit,
) {
    val albumArtistAlbums by viewModel.albumArtistAlbums.collectAsStateWithLifecycle()
    val nonAlbumArtistAlbums by viewModel.nonAlbumArtistAlbums.collectAsStateWithLifecycle()
    val albumArtMap by viewModel.albumArtMap.collectAsStateWithLifecycle()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle(0)
    val totalDuration by viewModel.totalDuration.collectAsStateWithLifecycle(0.0)

    Column(modifier = modifier.verticalScroll(state = rememberScrollState())) {
        if (isInLandscapeMode()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtGrid(albumArtList = albumArtMap.map { it.value }, modifier = Modifier.width(140.dp))
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxHeight(),
                    content = {
                        Text(
                            text = viewModel.artist,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        ArtistScreenMeta(albumArtistAlbums.size, songCount, totalDuration)
                    },
                )
            }
        } else {
            FadingImageBox(
                modifier = Modifier.fillMaxWidth(),
                image = { AlbumArtGrid(albumArtList = albumArtMap.map { it.value }) },
                topContent = {},
                bottomContent = {
                    Text(
                        text = viewModel.artist,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        content = { ArtistScreenMeta(albumArtistAlbums.size, songCount, totalDuration) },
                    )
                },
            )
        }

        if (albumArtistAlbums.isNotEmpty()) {
            Text(
                text = stringResource(R.string.albums),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(10.dp),
            )
            albumArtistAlbums.forEach { album ->
                AlbumRow(
                    album = album,
                    thumbnail = albumArtMap[album.album.name]?.thumbnail,
                    showArtist = album.album.artist != viewModel.artist,
                    onGotoAlbumClick = { onGotoAlbumClick(album.album) },
                )
            }
        }

        if (nonAlbumArtistAlbums.isNotEmpty()) {
            Text(
                text = stringResource(R.string.appears_on),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(10.dp),
            )
            nonAlbumArtistAlbums.forEach { album ->
                AlbumRow(
                    album = album,
                    thumbnail = albumArtMap[album.album.name]?.thumbnail,
                    showArtist = album.album.artist != viewModel.artist,
                    onGotoAlbumClick = { onGotoAlbumClick(album.album) },
                )
            }
        }
    }
}
