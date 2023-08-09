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
    val searchFilter: MPDFilter = mpdFilter { equals("album", name) and equals("albumartist", artist) }

    override fun equals(other: Any?) =
        other is MPDAlbum && other.artist == artist && other.name == name

    override fun hashCode() = 31 * artist.hashCode() + name.hashCode()
    override fun toString() = "${javaClass.simpleName}[artist: $artist, name: $name]"

    fun toProto(): MPDAlbumProto? = MPDAlbumProto.newBuilder()
        .setArtist(artist)
        .setName(name)
        .build()
}

fun Map<String, List<String>>.toMPDAlbums(artist: String): List<MPDAlbum> = try {
    val getAlbums: (String) -> List<String>? =
        { key -> this[key]?.filter { it.isNotBlank() }?.takeUnless { it.isEmpty() } }
    val albums = getAlbums("AlbumSort") ?: getAlbums("Album")

    albums!!.map { MPDAlbum(artist, it) }
} catch (e: NullPointerException) {
    emptyList()
}

fun Map<String, List<String>>.toMPDAlbums(): List<MPDAlbum> = try {
    val getArtist: (String) -> String? =
        { key -> this[key]?.firstOrNull()?.takeIf { it.isNotBlank() } }
    val artist =
        getArtist("AlbumArtistSort") ?: getArtist("AlbumArtist") ?: getArtist("ArtistSort") ?: getArtist("Artist")

    toMPDAlbums(artist!!)
} catch (e: NullPointerException) {
    emptyList()
}

fun Iterable<MPDAlbum>.sorted() = sortedBy { it.name.lowercase().replaceLeadingJunk() }
