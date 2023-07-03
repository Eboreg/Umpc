package us.huseli.umpc.data

data class MPDSong(
    val filename: String,
    val id: Int?,
    val artist: String,
    val albumArtist: String,
    val title: String,
    val album: String,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Double?,
    val year: Int?,
    val audioFormat: MPDAudioFormat?,
) {
    val albumArtKey = AlbumArtKey(albumArtist, album, filename)

    override fun equals(other: Any?) = other is MPDSong && other.filename == filename
    override fun hashCode() = filename.hashCode()
}

fun Map<String, String>.toMPDSong() = try {
    // The Date field is almost always a year. So even if it's a complete date,
    // just parse it as a year anyway.
    val parseYear: (String?) -> Int? = { string ->
        string?.let { Regex("^([1-2]\\d{3})").find(it)?.value?.toInt() }
    }

    MPDSong(
        filename = this["file"]!!,
        id = this["Id"]?.toInt(),
        artist = this["Artist"] ?: "(Unknown artist)",
        albumArtist = this["AlbumArtist"] ?: this["Artist"] ?: "(Unknown artist)",
        title = this["Title"] ?: "(Unknown title)",
        album = this["Album"] ?: "(Unknown album)",
        trackNumber = this["Track"]?.toInt(),
        discNumber = this["Disc"]?.toInt(),
        duration = this["duration"]?.toDouble(),
        year = parseYear(this["Date"]),
        audioFormat = this["Format"]?.toMPDAudioFormat(),
    )
} catch (e: NullPointerException) {
    null
}

fun Iterable<MPDSong>.sorted(): List<MPDSong> =
    this.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

fun Iterable<MPDSong>.filterByAlbum(artist: String, album: String): List<MPDSong> =
    this.filter { it.album == album && it.albumArtist == artist }.sorted()

fun Iterable<MPDSong>.filterByAlbum(album: MPDAlbum): List<MPDSong> =
    this.filter { it.album == album.name && it.albumArtist == album.artist }.sorted()

fun Iterable<MPDSong>.groupByAlbum(): List<MPDAlbumWithSongs> =
    this.groupBy { MPDAlbum(it.albumArtist, it.album) }.map { MPDAlbumWithSongs(it.key, it.value.sorted()) }
