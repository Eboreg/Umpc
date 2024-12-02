package us.huseli.umpc.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.viewmodels.LibraryViewModel

@Composable
fun LibraryScreenAlbumRow(
    viewModel: LibraryViewModel,
    album: MPDAlbum,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    val albumWithSongs by viewModel.getAlbumWithSongsFlow(album)
        .collectAsStateWithLifecycle(MPDAlbumWithSongs(album, emptyList()))

    LaunchedEffect(albumWithSongs) {
        viewModel.getAlbumArt(albumWithSongs) { thumbnail = it.thumbnail }
    }

    AlbumRow(
        album = albumWithSongs,
        thumbnail = thumbnail,
        showArtist = true,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}