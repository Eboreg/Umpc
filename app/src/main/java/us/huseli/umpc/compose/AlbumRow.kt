package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowOutward
import androidx.compose.material.icons.sharp.PlayArrow
import androidx.compose.material.icons.sharp.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import us.huseli.umpc.R
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.formatDuration
import us.huseli.umpc.isInLandscapeMode
import us.huseli.umpc.toYearRangeString

@Composable
fun AlbumRow(
    modifier: Modifier = Modifier,
    album: MPDAlbum,
    thumbnail: ImageBitmap? = null,
    showArtist: Boolean = false,
    duration: String? = null,
    years: String? = null,
    onEnqueueClick: () -> Unit,
    onPlayClick: () -> Unit,
    onGotoAlbumClick: () -> Unit,
    expandedContent: @Composable() (ColumnScope.() -> Unit),
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .padding(end = 10.dp)
            .clickable { isExpanded = !isExpanded }
            .height(IntrinsicSize.Min)
            .heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AlbumArt(imageBitmap = thumbnail, forceSquare = true)

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
                        Text(
                            text = album.name,
                            style = if (album.name.length >= 60 && !isInLandscapeMode()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
                        )
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
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallOutlinedButton(onClick = onEnqueueClick) {
                            Text(stringResource(R.string.enqueue), modifier = Modifier.padding(end = 4.dp))
                            Icon(Icons.Sharp.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        SmallOutlinedButton(onClick = onPlayClick) {
                            Text(stringResource(R.string.play), modifier = Modifier.padding(end = 4.dp))
                            Icon(Icons.Sharp.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onGotoAlbumClick) {
                        Icon(Icons.Sharp.ArrowOutward, stringResource(R.string.go_to_album))
                    }
                }
            }
        }
    }

    if (isExpanded) {
        CompositionLocalProvider(LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 2.dp) {
            Surface { Column { expandedContent() } }
        }
    }
}


@Composable
fun AlbumRow(
    modifier: Modifier = Modifier,
    album: MPDAlbumWithSongs,
    thumbnail: ImageBitmap? = null,
    showArtist: Boolean = false,
    onEnqueueClick: () -> Unit,
    onPlayClick: () -> Unit,
    onGotoAlbumClick: () -> Unit,
    expandedContent: @Composable() (ColumnScope.() -> Unit),
) {
    AlbumRow(
        modifier = modifier,
        album = album.album,
        thumbnail = thumbnail,
        showArtist = showArtist,
        duration = album.duration?.formatDuration(),
        years = album.yearRange?.toYearRangeString(),
        onEnqueueClick = onEnqueueClick,
        onPlayClick = onPlayClick,
        onGotoAlbumClick = onGotoAlbumClick,
        expandedContent = expandedContent,
    )
}
