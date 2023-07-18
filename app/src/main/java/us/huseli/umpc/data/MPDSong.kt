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
    val queuePosition: Int?,
) : Parcelable {
    @IgnoredOnParcel
    @Transient
    val albumArtKey = AlbumArtKey(album.artist, album.name, filename)

    override fun equals(other: Any?) = other is MPDSong && other.filename == filename
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
        return builder.build()
    }
}

fun Map<String, String>.toMPDSong() = try {
    MPDSong(
        filename = this["file"]!!,
        id = this["Id"]?.toInt(),
        artist = this["Artist"]!!,
        title = (this["Title"] ?: Paths.get(this["file"]!!).nameWithoutExtension),
        album = MPDAlbum((this["AlbumArtist"] ?: this["Artist"])!!, this["Album"]!!),
        trackNumber = this["Track"]?.toInt(),
        discNumber = this["Disc"]?.toInt(),
        duration = this["duration"]?.toDouble(),
        year = this["Date"]?.parseYear(),
        audioFormat = this["Format"]?.toMPDAudioFormat(),
        queuePosition = this["Pos"]?.toInt(),
    )
} catch (e: NullPointerException) {
    Logger.log("MPDSong", "$e, $this", Log.ERROR)
    null
}

fun Iterable<MPDSong>.sorted(): List<MPDSong> =
    this.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

fun Iterable<MPDSong>.groupByAlbum(): List<MPDAlbumWithSongs> =
    this.groupBy { it.album }.map { MPDAlbumWithSongs(it.key, it.value.sorted()) }

fun Iterable<MPDSong>.toNative(): List<MPDSongProto> = this.mapNotNull { it.toProto() }
