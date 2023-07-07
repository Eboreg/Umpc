package us.huseli.umpc.data

import java.time.Instant

open class MPDPlaylist(val name: String, val lastModified: Instant? = null) {
    override fun equals(other: Any?) = other is MPDPlaylist && other.name == name
    override fun hashCode() = name.hashCode()
}

class MPDPlaylistWithSongs(val playlist: MPDPlaylist, val songs: List<MPDSong>) {
    override fun equals(other: Any?) =
        other is MPDPlaylistWithSongs && other.playlist == playlist && other.songs == songs

    override fun hashCode(): Int = 31 * playlist.hashCode() + songs.hashCode()
    operator fun component1() = playlist
    operator fun component2() = songs
}

fun Map<String, String>.toMPDPlaylist() = try {
    val lastModified = this["Last-Modified"]?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            null
        }
    }

    MPDPlaylist(
        name = this["playlist"]!!,
        lastModified = lastModified,
    )
} catch (e: NullPointerException) {
    null
}
