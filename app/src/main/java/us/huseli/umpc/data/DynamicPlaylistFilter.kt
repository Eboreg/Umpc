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
    @Suppress("unused")
    enum class Key(val displayName: String, val mpdTag: String) {
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
        get() = mpdFilter {
            when (comparator) {
                Comparator.EQUALS -> equals(key.mpdTag, value)
                Comparator.NOT_EQUALS -> notEquals(key.mpdTag, value)
                Comparator.CONTAINS -> contains(key.mpdTag, value)
                Comparator.NOT_CONTAINS -> contains(key.mpdTag, value).not()
            }
        }

    override fun toString() = "${key.displayName} ${comparator.displayName} \"$value\""

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
}
