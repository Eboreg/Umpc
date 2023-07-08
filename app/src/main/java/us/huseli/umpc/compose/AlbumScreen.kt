package us.huseli.umpc.compose

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
import androidx.compose.material.icons.sharp.QueueMusic
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.viewmodels.AlbumViewModel

@Composable
fun AlbumScreenMetaButtons(
    album: MPDAlbum,
    onEnqueueClick: (MPDAlbum) -> Unit,
    onPlayClick: (MPDAlbum) -> Unit,
    onAddToPlaylistClick: (MPDAlbum) -> Unit,
) {
    SmallOutlinedButton(
        onClick = { onEnqueueClick(album) },
        leadingIcon = Icons.Sharp.QueueMusic,
        content = { Text(stringResource(R.string.enqueue)) },
    )
    SmallOutlinedButton(
        onClick = { onPlayClick(album) },
        leadingIcon = Icons.Sharp.PlayArrow,
        content = { Text(stringResource(R.string.play)) },
    )
    SmallOutlinedButton(
        onClick = { onAddToPlaylistClick(album) },
        leadingIcon = Icons.Sharp.PlaylistAdd,
        content = { Text(stringResource(R.string.add_to_playlist)) }
    )
}

@Composable
fun AlbumScreenMeta(
    album: MPDAlbum,
    onGotoArtistClick: (String) -> Unit,
    onEnqueueClick: (MPDAlbum) -> Unit,
    onPlayClick: (MPDAlbum) -> Unit,
    onAddToPlaylistClick: (MPDAlbum) -> Unit,
) {
    Text(
        text = album.name,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
    )
    Text(
        text = album.artist,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable { onGotoArtistClick(album.artist) },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 10.dp)) {
        AlbumScreenMetaButtons(
            album = album,
            onEnqueueClick = onEnqueueClick,
            onPlayClick = onPlayClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
        )
    }
}

@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
    onGotoArtistClick: (String) -> Unit,
) {
    val albumArt by viewModel.albumArt.collectAsStateWithLifecycle()
    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val currentSongFilename by viewModel.currentSongFilename.collectAsStateWithLifecycle(null)
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var isAddToPlaylistDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isAddToPlaylistDialogOpen) {
        val successMessage = stringResource(R.string.album_was_added_to_playlist)

        AddAlbumToPlaylistDialog(
            album = viewModel.album,
            playlists = playlists,
            onConfirm = {
                viewModel.addToPlaylist(it) { response ->
                    if (response.isSuccess) viewModel.addMessage(successMessage)
                    else response.error?.let { error -> viewModel.addMessage(error) }
                }
                isAddToPlaylistDialogOpen = false
            },
            onCancel = { isAddToPlaylistDialogOpen = false },
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
                        onGotoArtistClick = { onGotoArtistClick(it) },
                        onEnqueueClick = { viewModel.enqueueAlbum(it) },
                        onPlayClick = { viewModel.playAlbum(it) },
                        onAddToPlaylistClick = { isAddToPlaylistDialogOpen = true },
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
                        onGotoArtistClick = { onGotoArtistClick(it) },
                        onEnqueueClick = { viewModel.enqueueAlbum(it) },
                        onPlayClick = { viewModel.playAlbum(it) },
                        onAddToPlaylistClick = { isAddToPlaylistDialogOpen = true },
                    )
                }
            )
        }

        albumWithSongs?.songs?.forEach { song ->
            Divider()
            SmallSongRow(
                song = song,
                isCurrentSong = currentSongFilename == song.filename,
                playerState = playerState,
                onEnqueueClick = { viewModel.enqueueSong(song) },
                onPlayPauseClick = { viewModel.playOrPauseSong(song) },
                onGotoArtistClick = { onGotoArtistClick(song.artist) },
            )
        }
    }
}
