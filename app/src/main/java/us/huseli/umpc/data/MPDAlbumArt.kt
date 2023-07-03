package us.huseli.umpc.data

import androidx.compose.ui.graphics.ImageBitmap

data class MPDAlbumArt(
    val key: AlbumArtKey,
    val fullImage: ImageBitmap,
    val thumbnail: ImageBitmap? = null,
) {
    override fun equals(other: Any?) = other is MPDAlbumArt && other.key == key
    override fun hashCode() = key.hashCode()
}
