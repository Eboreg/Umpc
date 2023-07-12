package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.mpd.mpdFilter

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
}
