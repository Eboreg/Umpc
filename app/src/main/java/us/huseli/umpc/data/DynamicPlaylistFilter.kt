package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.mpdFilter
import us.huseli.umpc.proto.DynamicPlaylistProto

@Parcelize
data class DynamicPlaylistFilter(
    val key: Key = Key.ARTIST,
    val value: String = "",
    val comparator: Comparator = Comparator.EQUALS,
) : Parcelable {
    enum class Key(val display: String, val mpdTag: String) {
        ARTIST("Artist", "artist"),
        ALBUM_ARTIST("Album artist", "albumartist"),
        ALBUM("Album", "album"),
        SONG_TITLE("Song title", "title"),
        FILENAME("File path", "file"),
    }

    enum class Comparator(val displayName: String) {
        EQUALS("equals"),
        NOT_EQUALS("does not equal"),
        CONTAINS("contains"),
        NOT_CONTAINS("does not contain"),
    }

    @IgnoredOnParcel
    val mpdFilter: MPDFilter
        get() = when (comparator) {
            Comparator.EQUALS -> mpdFilter { key.mpdTag eq value }
            Comparator.NOT_EQUALS -> mpdFilter { key.mpdTag ne value }
            Comparator.CONTAINS -> mpdFilter { key.mpdTag contains value }
            Comparator.NOT_CONTAINS -> mpdFilter { !(key.mpdTag contains value) }
        }

    fun toProto(): DynamicPlaylistProto.Filter = DynamicPlaylistProto.Filter
        .newBuilder()
        .setKey(
            when (key) {
                Key.ARTIST -> DynamicPlaylistProto.Key.ARTIST
                Key.ALBUM_ARTIST -> DynamicPlaylistProto.Key.ALBUM_ARTIST
                Key.ALBUM -> DynamicPlaylistProto.Key.ALBUM
                Key.SONG_TITLE -> DynamicPlaylistProto.Key.SONG_TITLE
                Key.FILENAME -> DynamicPlaylistProto.Key.FILENAME
            }
        )
        .setComparator(
            when (comparator) {
                Comparator.EQUALS -> DynamicPlaylistProto.Comparator.EQUALS
                Comparator.NOT_EQUALS -> DynamicPlaylistProto.Comparator.NOT_EQUALS
                Comparator.CONTAINS -> DynamicPlaylistProto.Comparator.CONTAINS
                Comparator.NOT_CONTAINS -> DynamicPlaylistProto.Comparator.NOT_CONTAINS
            }
        )
        .setValue(value)
        .build()

    override fun toString() = "${key.display} ${comparator.displayName} \"$value\""

    companion object {
        fun comparatorValuesByVersion(protocolVersion: MPDVersion): List<Comparator> =
            if (protocolVersion >= MPDVersion("0.21")) Comparator.values().toList()
            else listOf(Comparator.CONTAINS)
    }
}
