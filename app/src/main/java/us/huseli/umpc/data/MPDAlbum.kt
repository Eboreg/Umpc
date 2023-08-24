package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.mpdFilter
import us.huseli.umpc.replaceLeadingJunk

@Parcelize
data class MPDAlbum(val artist: String, val name: String) : Parcelable {
    override fun equals(other: Any?) = when (other) {
        is MPDAlbum -> other.artist == artist && other.name == name
        is MPDAlbumWithSongs -> other.album.artist == artist && other.album.name == name
        else -> false
    }

    override fun hashCode() = 31 * artist.hashCode() + name.hashCode()
    override fun toString() = "${javaClass.simpleName}[artist: $artist, name: $name]"

    fun getMPDFilter(artistTag: String): MPDFilter =
        mpdFilter { ("album" eq name) and (artistTag eq artist) }

    fun getMPDFilter(): MPDFilter = getMPDFilter("albumartist")
}

fun Map<String, List<String>>.toMPDAlbums(artist: String): List<MPDAlbum> {
    val getAlbums: (String) -> List<String>? =
        { key -> this[key]?.filter { it.isNotBlank() }?.takeUnless { it.isEmpty() } }
    val albums = getAlbums("albumsort") ?: getAlbums("album")

    return albums?.map { MPDAlbum(artist, it) } ?: emptyList()
}

fun Map<String, List<String>>.toMPDAlbums(): List<MPDAlbum> {
    val getArtist: (String) -> String? =
        { key -> this[key]?.firstOrNull()?.takeIf { it.isNotBlank() } }
    val artist =
        getArtist("albumartistsort") ?: getArtist("albumartist") ?: getArtist("artistsort") ?: getArtist("artist")

    return artist?.let { toMPDAlbums(it) } ?: emptyList()
}

fun Iterable<MPDAlbum>.sorted() = sortedBy { it.name.lowercase().replaceLeadingJunk() }
