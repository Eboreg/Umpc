package us.huseli.umpc.data

import android.util.Log
import us.huseli.umpc.Logger
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

data class MPDSong(
    val filename: String,
    val id: Int?,
    val artist: String,
    val title: String,
    val album: MPDAlbum,
    val trackNumber: Int?,
    val discNumber: Int?,
    val duration: Double?,
    val year: Int?,
    val audioFormat: MPDAudioFormat?,
) {
    val albumArtKey = AlbumArtKey(album.artist, album.name, filename)

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
        artist = this["Artist"]!!,
        title = (this["Title"] ?: Paths.get(this["file"]!!).nameWithoutExtension),
        album = MPDAlbum((this["AlbumArtist"] ?: this["Artist"])!!, this["Album"]!!),
        trackNumber = this["Track"]?.toInt(),
        discNumber = this["Disc"]?.toInt(),
        duration = this["duration"]?.toDouble(),
        year = parseYear(this["Date"]),
        audioFormat = this["Format"]?.toMPDAudioFormat(),
    )
} catch (e: NullPointerException) {
    Logger.log("MPDSong", "$e, $this", Log.ERROR)
    null
}

fun Iterable<MPDSong>.sorted(): List<MPDSong> =
    this.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

fun Iterable<MPDSong>.filterByAlbum(artist: String, album: String): List<MPDSong> =
    this.filter { it.album.name == album && it.album.artist == artist }.sorted()

fun Iterable<MPDSong>.filterByAlbum(album: MPDAlbum): List<MPDSong> =
    this.filter { it.album == album }.sorted()

fun Iterable<MPDSong>.groupByAlbum(): List<MPDAlbumWithSongs> =
    this.groupBy { it.album }.map { MPDAlbumWithSongs(it.key, it.value.sorted()) }
