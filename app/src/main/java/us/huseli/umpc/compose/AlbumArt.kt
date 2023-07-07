package us.huseli.umpc.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun AlbumArt(
    imageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    altIcon: ImageVector = Icons.Sharp.Album,
    forceSquare: Boolean = false,
) {
    val boxModifier = if (forceSquare) modifier.aspectRatio(1f) else modifier.fillMaxWidth()

    Box(modifier = boxModifier) {
        val imageModifier =
            if (forceSquare) Modifier.matchParentSize().align(Alignment.Center) else Modifier.fillMaxWidth()

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = imageModifier,
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = altIcon,
                contentDescription = null,
                modifier = imageModifier.aspectRatio(1f),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
