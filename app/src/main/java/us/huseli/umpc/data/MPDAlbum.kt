package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.mpdFilter
import us.huseli.umpc.proto.MPDAlbumProto
import us.huseli.umpc.replaceLeadingJunk

@Parcelize
data class MPDAlbum(val artist: String, val name: String) : Parcelable {
    @IgnoredOnParcel
    @Transient
    val searchFilter: MPDFilter = mpdFilter { equals("album", name).and(equals("albumartist", artist)) }

    override fun equals(other: Any?) =
        other is MPDAlbum && other.artist == artist && other.name == name

    override fun hashCode() = 31 * artist.hashCode() + name.hashCode()
    override fun toString() = "${javaClass.simpleName}[artist: $artist, name: $name]"

    fun toProto(): MPDAlbumProto? = MPDAlbumProto.newBuilder()
        .setArtist(artist)
        .setName(name)
        .build()
}

data class MPDAlbumWithSongs(val album: MPDAlbum, val songs: List<MPDSong>) {
    val albumArtKey: AlbumArtKey?
        get() = songs.firstOrNull()?.albumArtKey

    val duration: Double? = songs.mapNotNull { it.duration }.takeIf { it.isNotEmpty() }?.sum()
    val yearRange: IntRange? =
        songs.mapNotNull { it.year }.takeIf { it.isNotEmpty() }?.let { IntRange(it.min(), it.max()) }
}

fun Map<String, List<String>>.toMPDAlbums(): List<MPDAlbum> = try {
    val getArtist: (String) -> String? =
        { key -> this[key]?.firstOrNull()?.takeIf { it.isNotBlank() } }
    val getAlbums: (String) -> List<String>? =
        { key -> this[key]?.filter { it.isNotBlank() }?.takeUnless { it.isEmpty() } }
    val artist =
        getArtist("AlbumArtistSort") ?: getArtist("AlbumArtist") ?: getArtist("ArtistSort") ?: getArtist("Artist")
    val albums = getAlbums("AlbumSort") ?: getAlbums("Album")

    albums!!.map { MPDAlbum(artist!!, it) }
} catch (e: NullPointerException) {
    emptyList()
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

fun Iterable<MPDAlbum>.sorted() = sortedBy { it.name.lowercase().replaceLeadingJunk() }
