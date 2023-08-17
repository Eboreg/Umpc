package us.huseli.umpc.data

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.Logger
import us.huseli.umpc.parseYear
import us.huseli.umpc.proto.MPDSongProto
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

    fun toProto(): MPDSongProto? {
        val builder = MPDSongProto.newBuilder()
            .setFilename(filename)
            .setArtist(artist)
            .setTitle(title)
            .setAlbum(album.toProto())
        if (id != null) builder.id = id
        if (trackNumber != null) builder.trackNumber = trackNumber
        if (discNumber != null) builder.discNumber = discNumber
        if (duration != null) builder.duration = duration
        if (year != null) builder.year = year
        if (position != null) builder.position = position
        return builder.build()
    }
}

fun Map<String, String>.toMPDSong(position: Int? = null) = try {
    MPDSong(
        filename = this["file"]!!,
        id = this["Id"]?.toInt(),
        artist = this["Artist"] ?: "(Unknown)",
        title = (this["Title"] ?: Paths.get(this["file"]!!).nameWithoutExtension),
        album = MPDAlbum(this["AlbumArtist"] ?: this["Artist"] ?: "(Unknown)", this["Album"] ?: "(Unknown)"),
        trackNumber = this["Track"]?.split("/")?.first()?.toInt(),
        discNumber = this["Disc"]?.toInt(),
        duration = this["duration"]?.toDouble(),
        year = this["Date"]?.parseYear(),
        audioFormat = this["Format"]?.toMPDAudioFormat(),
        position = position ?: this["Pos"]?.toInt(),
    )
} catch (e: NullPointerException) {
    Logger.log("MPDSong", "$e, $this", Log.ERROR)
    null
}

fun List<Map<String, String>>.toMPDSongs(): List<MPDSong> = mapNotNull { it.toMPDSong() }

fun Iterable<MPDSong>.sorted(): List<MPDSong> = sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

fun Iterable<MPDSong>.groupByAlbum(): List<MPDAlbumWithSongs> =
    groupBy { it.album }.map { MPDAlbumWithSongs(it.key, it.value.sorted()) }

fun Iterable<MPDSong>.toProto(): List<MPDSongProto> = mapNotNull { it.toProto() }
