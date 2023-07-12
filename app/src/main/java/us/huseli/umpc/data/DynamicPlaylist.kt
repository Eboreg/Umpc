package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.Instant

@Parcelize
data class DynamicPlaylist(
    val filter: DynamicPlaylistFilter,
    val shuffle: Boolean = false,
    val lastModified: Instant = Instant.now(),
) : Parcelable {
    override fun equals(other: Any?) = other is DynamicPlaylist && other.filter == filter
    override fun hashCode() = filter.hashCode()
    override fun toString() = filter.toString()
}
