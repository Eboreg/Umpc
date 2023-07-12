package us.huseli.umpc.data

data class AlbumArtKey(val albumArtist: String, val album: String, val filename: String) {
    val imageFilename = hashCode().toString()

    override fun hashCode() = 31 * albumArtist.hashCode() + album.hashCode()
    override fun equals(other: Any?) = other is AlbumArtKey && other.hashCode() == hashCode()
}
