package us.huseli.umpc.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import us.huseli.umpc.data.MPDAlbumWithSongs
import us.huseli.umpc.formatDuration
import us.huseli.umpc.toYearRangeString

@Composable
fun AlbumRow(
    modifier: Modifier = Modifier,
    album: MPDAlbumWithSongs,
    thumbnail: ImageBitmap? = null,
    showArtist: Boolean = false,
    onGotoAlbumClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(end = 10.dp)
            .clickable { onGotoAlbumClick() }
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
                    Text(
                        text = album.album.name,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    if (showArtist) {
                        Text(
                            text = album.album.artist,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Column(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.End,
                ) {
                    album.duration?.let { duration ->
                        Text(
                            text = duration.formatDuration(),
                            style = MaterialTheme.typography.labelSmall,
                            softWrap = false,
                            maxLines = 1,
                        )
                    }
                    album.yearRange?.let { yearRange ->
                        Text(
                            text = yearRange.toYearRangeString(),
                            style = MaterialTheme.typography.labelSmall,
                            softWrap = false,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
