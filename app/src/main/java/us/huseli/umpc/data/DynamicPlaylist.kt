package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.mpd.mpdFilter
import java.time.Instant

enum class DynamicPlaylistFilterKey(val displayName: String, val mpdTag: String) {
    ARTIST("Artist", "artist"),
    ALBUM_ARTIST("Album artist", "albumartist"),
    ALBUM("Album", "album"),
    SONG_TITLE("Song title", "title"),
    FILENAME("File path", "file"),
}

enum class DynamicPlaylistFilterComparator(val displayName: String) {
    EQUALS("equals"),
    NOT_EQUALS("does not equal"),
    CONTAINS("contains"),
    NOT_CONTAINS("does not contain"),
}

@Parcelize
data class DynamicPlaylistFilter(
    val key: DynamicPlaylistFilterKey = DynamicPlaylistFilterKey.ARTIST,
    val value: String = "",
    val comparator: DynamicPlaylistFilterComparator = DynamicPlaylistFilterComparator.EQUALS,
) : Parcelable {
    @IgnoredOnParcel
    val mpdFilter = mpdFilter {
        when (comparator) {
            DynamicPlaylistFilterComparator.EQUALS -> equals(key.mpdTag, value)
            DynamicPlaylistFilterComparator.NOT_EQUALS -> notEquals(key.mpdTag, value)
            DynamicPlaylistFilterComparator.CONTAINS -> contains(key.mpdTag, value)
            DynamicPlaylistFilterComparator.NOT_CONTAINS -> contains(key.mpdTag, value).not()
        }
    }
}

@Parcelize
data class DynamicPlaylist(
    val name: String,
    val filter: DynamicPlaylistFilter,
    val shuffle: Boolean = false,
    val lastModified: Instant = Instant.now(),
) : Parcelable {
    override fun equals(other: Any?) = other is DynamicPlaylist && other.name == name
    override fun hashCode() = name.hashCode()
}
