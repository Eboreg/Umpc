package us.huseli.umpc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import us.huseli.umpc.proto.DynamicPlaylistProto
import java.io.InputStream
import java.io.OutputStream

object DynamicPlaylistSerializer : Serializer<DynamicPlaylistProto> {
    override val defaultValue: DynamicPlaylistProto = DynamicPlaylistProto.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): DynamicPlaylistProto = DynamicPlaylistProto.parseFrom(input)
    override suspend fun writeTo(t: DynamicPlaylistProto, output: OutputStream) = t.writeTo(output)
}

val Context.dynamicPlaylistDataStore:
    DataStore<DynamicPlaylistProto> by dataStore("dynamicPlaylist.pb", DynamicPlaylistSerializer)

fun DynamicPlaylistProto.Filter.toNative(): DynamicPlaylistFilter = DynamicPlaylistFilter(
    key = when (key) {
        DynamicPlaylistProto.Key.ARTIST -> DynamicPlaylistFilter.Key.ARTIST
        DynamicPlaylistProto.Key.ALBUM_ARTIST -> DynamicPlaylistFilter.Key.ALBUM_ARTIST
        DynamicPlaylistProto.Key.ALBUM -> DynamicPlaylistFilter.Key.ALBUM
        DynamicPlaylistProto.Key.SONG_TITLE -> DynamicPlaylistFilter.Key.SONG_TITLE
        DynamicPlaylistProto.Key.FILENAME -> DynamicPlaylistFilter.Key.FILENAME
        else -> throw Exception("Unrecognised key")
    },
    value = value,
    comparator = when (comparator) {
        DynamicPlaylistProto.Comparator.EQUALS -> DynamicPlaylistFilter.Comparator.EQUALS
        DynamicPlaylistProto.Comparator.NOT_EQUALS -> DynamicPlaylistFilter.Comparator.NOT_EQUALS
        DynamicPlaylistProto.Comparator.CONTAINS -> DynamicPlaylistFilter.Comparator.CONTAINS
        DynamicPlaylistProto.Comparator.NOT_CONTAINS -> DynamicPlaylistFilter.Comparator.NOT_CONTAINS
        else -> throw Exception("Unrecognised comparator")
    },
)

fun DynamicPlaylistProto.toNative(): DynamicPlaylist = DynamicPlaylist(
    filters = filtersList.map { it.toNative() },
    shuffle = shuffle,
    songCount = filenamesCount,
    operator = when (operator) {
        DynamicPlaylistProto.Operator.AND -> DynamicPlaylist.Operator.AND
        DynamicPlaylistProto.Operator.OR -> DynamicPlaylist.Operator.OR
        else -> throw Exception("Unrecognised operator")
    },
    server = MPDServer.fromString(server),
)
