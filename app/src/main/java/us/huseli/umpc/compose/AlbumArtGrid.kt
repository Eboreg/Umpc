package us.huseli.umpc.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import us.huseli.umpc.data.MPDAlbumArt
import us.huseli.umpc.roundUpSqrt

@Composable
fun AlbumArtGrid(modifier: Modifier = Modifier, albumArtList: Collection<MPDAlbumArt>) {
    val itemsPerRow = albumArtList.size.roundUpSqrt()
    val screenWidth = LocalContext.current.resources.configuration.screenWidthDp.dp

    if (itemsPerRow > 0) {
        Column(modifier = modifier.fillMaxWidth()) {
            albumArtList.chunked(itemsPerRow).forEachIndexed { index, sublist ->
                if (sublist.size > 1 || index == 0) {
                    Row(modifier = Modifier.height(screenWidth / sublist.size)) {
                        sublist.forEach { albumArt ->
                            Image(
                                bitmap = albumArt.fullImage,
                                contentDescription = null,
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier.width(screenWidth / sublist.size).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}
