package us.huseli.umpc.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

@Composable
fun AlbumArt(
    imageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    altIcon: ImageVector = Icons.Sharp.Album,
) {
    val boxModifier = if (size != null) modifier.size(size) else modifier

    Box(modifier = boxModifier.aspectRatio(1f)) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
            )
        } else {
            Icon(
                imageVector = altIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
