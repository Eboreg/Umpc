package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.formatDuration
import us.huseli.umpc.toYearRangeString

@Composable
fun AlbumRow(
    modifier: Modifier = Modifier,
    album: MPDAlbum,
    thumbnail: ImageBitmap? = null,
    showArtist: Boolean = false,
    duration: String? = null,
    years: String? = null,
    collapsedHeight: Dp = 54.dp,
    expandedHeight: Dp = 108.dp,
    onEnqueueClick: () -> Unit,
    onPlayClick: () -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .height(if (isExpanded) expandedHeight else collapsedHeight)
            .padding(end = 8.dp)
            .clickable { isExpanded = !isExpanded },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AlbumArt(imageBitmap = thumbnail)

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (showArtist) {
                        AutoScrollingTextLine(album.name)
                        AutoScrollingTextLine(album.artist, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(text = album.name)
                    }
                }
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.End,
                ) {
                    if (duration != null) {
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            softWrap = false,
                            maxLines = 1,
                        )
                    }
                    if (years != null) {
                        Text(
                            text = years,
                            style = MaterialTheme.typography.labelSmall,
                            softWrap = false,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (isExpanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        modifier = Modifier.height(35.dp),
                        onClick = onEnqueueClick,
                        contentPadding = PaddingValues(8.dp, 0.dp),
                        shape = ShapeDefaults.ExtraSmall,
                    ) {
                        Text(stringResource(R.string.enqueue), modifier = Modifier.padding(end = 4.dp))
                        Icon(Icons.Sharp.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    OutlinedButton(
                        modifier = Modifier.height(35.dp),
                        onClick = onPlayClick,
                        contentPadding = PaddingValues(10.dp, 0.dp),
                        shape = ShapeDefaults.ExtraSmall,
                    ) {
                        Text(stringResource(R.string.play), modifier = Modifier.padding(end = 4.dp))
                        Icon(Icons.Sharp.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (isExpanded) {
        CompositionLocalProvider(
            LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 2.dp,
        ) { Surface { Column { expandedContent() } } }
    }
}


@Composable
fun AlbumRow(
    modifier: Modifier = Modifier,
    album: MPDAlbumWithSongs,
    thumbnail: ImageBitmap? = null,
    showArtist: Boolean = false,
    collapsedHeight: Dp = 54.dp,
    expandedHeight: Dp = 108.dp,
    onEnqueueClick: () -> Unit,
    onPlayClick: () -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    AlbumRow(
        modifier = modifier,
        album = album,
        thumbnail = thumbnail,
        showArtist = showArtist,
        collapsedHeight = collapsedHeight,
        expandedHeight = expandedHeight,
        onEnqueueClick = onEnqueueClick,
        onPlayClick = onPlayClick,
        years = album.yearRange?.toYearRangeString(),
        duration = album.duration?.formatDuration(),
        expandedContent = expandedContent,
    )
}
