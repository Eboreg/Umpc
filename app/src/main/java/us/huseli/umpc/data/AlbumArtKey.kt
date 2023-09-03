package us.huseli.umpc.data

data class AlbumArtKey(val artist: String, val album: String, val filename: String) {
    val imageFilename = hashCode().toString()

    override fun hashCode() = 31 * artist.hashCode() + album.hashCode()
    override fun equals(other: Any?) = other is AlbumArtKey && other.artist == artist && other.album == album
    override fun toString() = "AlbumArtKey[artist=$artist, album=$album, filename=$filename]"
}
