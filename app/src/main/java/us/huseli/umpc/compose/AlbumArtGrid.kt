package us.huseli.umpc.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import us.huseli.umpc.Logger
import us.huseli.umpc.roundUpSqrt

@Composable
fun AlbumArtGrid(modifier: Modifier = Modifier, albumArtList: List<ImageBitmap>) {
    val screenWidth = LocalContext.current.resources.configuration.screenWidthDp.dp
    val slicedAlbumArtList = when {
        albumArtList.size >= 16 -> albumArtList.subList(0, 16)
        albumArtList.size >= 12 -> albumArtList.subList(0, 12)
        albumArtList.size >= 9 -> albumArtList.subList(0, 9)
        albumArtList.size >= 6 -> albumArtList.subList(0, 6)
        albumArtList.size >= 4 -> albumArtList.subList(0, 4)
        albumArtList.size >= 2 -> albumArtList.subList(0, 2)
        else -> albumArtList
    }
    val itemsPerRow = slicedAlbumArtList.size.roundUpSqrt()

    if (itemsPerRow > 0) {
        Column(modifier = modifier.fillMaxWidth()) {
            slicedAlbumArtList.chunked(itemsPerRow).forEachIndexed { index, sublist ->
                if (sublist.size > 1 || index == 0) {
                    Row(modifier = Modifier.heightIn(min = 0.dp)) {
                        sublist.forEach { albumArt ->
                            Logger.log(
                                "AlbumArtGrid",
                                "albumArtList.size=${slicedAlbumArtList.size}, sublist.size = ${sublist.size}, index=$index, itemsPerRow=$itemsPerRow, screenWidth=$screenWidth, screenWidth / sublist.size = ${screenWidth / sublist.size}"
                            )
                            Image(
                                bitmap = albumArt,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(screenWidth / sublist.size)
                            )
                        }
                    }
                }
            }
        }
    }
}
