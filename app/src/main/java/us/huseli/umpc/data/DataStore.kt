package us.huseli.umpc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import us.huseli.umpc.proto.DynamicPlaylistProto
import us.huseli.umpc.proto.MPDSongProto
import us.huseli.umpc.proto.QueueProto
import java.io.InputStream
import java.io.OutputStream

object QueueSerializer : Serializer<QueueProto> {
    override val defaultValue: QueueProto = QueueProto.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): QueueProto = QueueProto.parseFrom(input)
    override suspend fun writeTo(t: QueueProto, output: OutputStream) = t.writeTo(output)
}

object DynamicPlaylistSerializer : Serializer<DynamicPlaylistProto> {
    override val defaultValue: DynamicPlaylistProto = DynamicPlaylistProto.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): DynamicPlaylistProto = DynamicPlaylistProto.parseFrom(input)
    override suspend fun writeTo(t: DynamicPlaylistProto, output: OutputStream) = t.writeTo(output)
}

val Context.queueDataStore: DataStore<QueueProto> by dataStore("queue.pb", QueueSerializer)

val Context.dynamicPlaylistDataStore:
    DataStore<DynamicPlaylistProto> by dataStore("dynamicPlaylist.pb", DynamicPlaylistSerializer)

fun MPDSongProto.toNative(): MPDSong = MPDSong(
    filename = filename,
    id = id,
    artist = artist,
    title = title,
    album = MPDAlbum(artist = album.artist, name = album.name),
    trackNumber = trackNumber.takeIf { it > 0 },
    discNumber = discNumber.takeIf { it > 0 },
    duration = duration.takeIf { it > 0 },
    year = year.takeIf { it > 0 },
    audioFormat = null,
    queuePosition = queuePosition,
)

fun QueueProto.toNative(): List<MPDSong> = this.songsList.map { it.toNative() }

fun DynamicPlaylistProto.Filter.toNative(): DynamicPlaylistFilter = DynamicPlaylistFilter(
    key = when (this.key) {
        DynamicPlaylistProto.Key.ARTIST -> DynamicPlaylistFilter.Key.ARTIST
        DynamicPlaylistProto.Key.ALBUM_ARTIST -> DynamicPlaylistFilter.Key.ALBUM_ARTIST
        DynamicPlaylistProto.Key.ALBUM -> DynamicPlaylistFilter.Key.ALBUM
        DynamicPlaylistProto.Key.SONG_TITLE -> DynamicPlaylistFilter.Key.SONG_TITLE
        DynamicPlaylistProto.Key.FILENAME -> DynamicPlaylistFilter.Key.FILENAME
        else -> throw Exception("Unrecognised key")
    },
    value = this.value,
    comparator = when (this.comparator) {
        DynamicPlaylistProto.Comparator.EQUALS -> DynamicPlaylistFilter.Comparator.EQUALS
        DynamicPlaylistProto.Comparator.NOT_EQUALS -> DynamicPlaylistFilter.Comparator.NOT_EQUALS
        DynamicPlaylistProto.Comparator.CONTAINS -> DynamicPlaylistFilter.Comparator.CONTAINS
        DynamicPlaylistProto.Comparator.NOT_CONTAINS -> DynamicPlaylistFilter.Comparator.NOT_CONTAINS
        else -> throw Exception("Unrecognised comparator")
    },
)

fun DynamicPlaylistProto.toNative(): DynamicPlaylist = DynamicPlaylist(
    filter = this.filter.toNative(),
    shuffle = this.shuffle,
)
