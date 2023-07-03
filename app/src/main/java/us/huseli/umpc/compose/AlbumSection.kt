package us.huseli.umpc.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.umpc.data.MPDAlbumWithSongs

@Composable
fun AlbumSection(
    modifier: Modifier = Modifier,
    album: MPDAlbumWithSongs,
    showArtist: Boolean = false,
    thumbnail: ImageBitmap? = null,
    collapsedHeight: Dp = 54.dp,
    expandedHeight: Dp = 108.dp,
    onAlbumEnqueueClick: () -> Unit,
    onAlbumPlayClick: () -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    AlbumRow(
        modifier = modifier,
        album = album,
        showArtist = showArtist,
        onPlayClick = onAlbumPlayClick,
        onEnqueueClick = onAlbumEnqueueClick,
        isExpanded = isExpanded,
        onClick = { isExpanded = !isExpanded },
        thumbnail = thumbnail,
        expandedHeight = expandedHeight,
        collapsedHeight = collapsedHeight,
    )

    if (isExpanded) {
        CompositionLocalProvider(
            LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 2.dp,
        ) { Surface { Column { expandedContent() } } }
    }
}
