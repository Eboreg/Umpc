package us.huseli.umpc.data

open class MPDAlbum(val artist: String, val name: String) {
    override fun equals(other: Any?) =
        other is MPDAlbum && other.artist == artist && other.name == name

    override fun hashCode() = 31 * artist.hashCode() + name.hashCode()
    override fun toString() = "${javaClass.simpleName}[artist: $artist, name: $name]"
}

class MPDAlbumWithSongs(artist: String, name: String, val songs: List<MPDSong>) : MPDAlbum(artist, name) {
    constructor(mpdAlbum: MPDAlbum, songs: List<MPDSong>) : this(mpdAlbum.artist, mpdAlbum.name, songs)

    val albumArtKey: AlbumArtKey
        get() = songs.firstOrNull()?.albumArtKey ?: AlbumArtKey(artist, name)

    val duration: Double? = songs.mapNotNull { it.duration }.takeIf { it.isNotEmpty() }?.sum()
    val yearRange: IntRange? =
        songs.mapNotNull { it.year }.takeIf { it.isNotEmpty() }?.let { IntRange(it.min(), it.max()) }

    fun copy(artist: String = this.artist, name: String = this.name, songs: List<MPDSong> = this.songs) =
        MPDAlbumWithSongs(artist, name, songs)

    override fun equals(other: Any?) =
        other is MPDAlbumWithSongs && super.equals(other) && other.songs == songs

    override fun hashCode(): Int = 31 * super.hashCode() + songs.hashCode()
}

fun Map<String, List<String>>.toMPDAlbums(): List<MPDAlbum> = try {
    val getArtist: (String) -> String? =
        { key -> this[key]?.firstOrNull()?.takeIf { it.isNotBlank() } }
    val getAlbums: (String) -> List<String>? =
        { key -> this[key]?.filter { it.isNotBlank() }?.takeUnless { it.isEmpty() } }
    val artist =
        getArtist("AlbumArtistSort") ?: getArtist("AlbumArtist") ?: getArtist("ArtistSort") ?: getArtist("Artist")
    val albums = getAlbums("AlbumSort") ?: getAlbums("Album")

    albums!!.map { MPDAlbum(artist!!, it) }.sortedBy { it.artist.lowercase() }
} catch (e: NullPointerException) {
    emptyList()
}

fun Iterable<MPDAlbum>.groupByArtist(): List<MPDArtistWithAlbums> =
    this.groupBy { it.artist }
        .map { MPDArtistWithAlbums(name = it.key, albums = it.value) }
        .sortedBy { it.name.lowercase() }

fun Iterable<MPDAlbumWithSongs>.sortedByYear(): List<MPDAlbumWithSongs> = this.sortedBy { it.yearRange?.first }
