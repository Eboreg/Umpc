package us.huseli.umpc.data

data class MPDAlbumWithSongs(val album: MPDAlbum, val songs: List<MPDSong>) {
    val albumArtKey: AlbumArtKey?
        get() = songs.firstOrNull()?.albumArtKey

    val duration: Double? = songs.mapNotNull { it.duration }.takeIf { it.isNotEmpty() }?.sum()
    val yearRange: IntRange? =
        songs.mapNotNull { it.year }.takeIf { it.isNotEmpty() }?.let { IntRange(it.min(), it.max()) }

    override fun equals(other: Any?) = when (other) {
        is MPDAlbum -> album == other
        is MPDAlbumWithSongs -> album == other.album && songs == other.songs
        else -> false
    }

    override fun hashCode(): Int = 31 * album.hashCode() + songs.hashCode()
}

fun Iterable<MPDAlbum>.groupByArtist(): List<MPDArtistWithAlbums> =
    groupBy { it.artist }
        .map { MPDArtistWithAlbums(name = it.key, albums = it.value) }
        .sorted()

fun Iterable<MPDAlbumWithSongs>.sortedByYear(): List<MPDAlbumWithSongs> = sortedBy { it.yearRange?.first }

fun Iterable<MPDAlbumWithSongs>.plus(other: Iterable<MPDAlbumWithSongs>) =
    associate { it.album to it.songs }
        .plus(other.associate { it.album to it.songs })
        .map { MPDAlbumWithSongs(it.key, it.value) }
