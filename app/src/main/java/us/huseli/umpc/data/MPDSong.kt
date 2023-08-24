package us.huseli.umpc.data

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.Logger
import us.huseli.umpc.parseYear
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

@Parcelize
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
    val position: Int?,
) : Parcelable {
    @IgnoredOnParcel
    @Transient
    val albumArtKey = AlbumArtKey(album.artist, album.name, filename)

    @IgnoredOnParcel
    @Transient
    val listKey = id ?: position ?: filename

    override fun equals(other: Any?): Boolean {
        if (other is MPDSong && other.filename == filename) {
            if (id != null) return other.id == id
            if (position != null) return other.position == position
            return true
        }
        return false
    }

    override fun hashCode() = filename.hashCode()
}

fun Map<String, String>.toMPDSong(position: Int? = null) = try {
    MPDSong(
        filename = this["file"]!!,
        id = this["id"]?.toInt(),
        artist = this["artist"] ?: "(Unknown)",
        title = (this["title"] ?: Paths.get(this["file"]!!).nameWithoutExtension),
        album = MPDAlbum(this["albumartist"] ?: this["artist"] ?: "(Unknown)", this["album"] ?: "(Unknown)"),
        trackNumber = this["track"]?.split("/")?.first()?.toInt(),
        discNumber = this["disc"]?.toInt(),
        duration = this["duration"]?.toDouble() ?: this["time"]?.toDouble(),
        year = this["date"]?.parseYear(),
        audioFormat = this["format"]?.toMPDAudioFormat(),
        position = position ?: this["pos"]?.toInt(),
    )
} catch (e: NullPointerException) {
    Logger.log("MPDSong", "$e, $this", Log.ERROR)
    null
}

fun Iterable<Map<String, String>>.toMPDSongs(): List<MPDSong> = mapNotNull { it.toMPDSong() }

fun Iterable<MPDSong>.sorted(): List<MPDSong> = sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

fun Iterable<MPDSong>.groupByAlbum(): List<MPDAlbumWithSongs> =
    groupBy { it.album }.map { MPDAlbumWithSongs(it.key, it.value.sorted()) }

