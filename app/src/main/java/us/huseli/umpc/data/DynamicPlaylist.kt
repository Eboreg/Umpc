package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.proto.DynamicPlaylistProto

@Parcelize
data class DynamicPlaylist(
    val filter: DynamicPlaylistFilter,
    val shuffle: Boolean = false,
) : Parcelable {
    override fun equals(other: Any?) =
        other is DynamicPlaylist && other.filter == filter && other.shuffle == shuffle

    override fun hashCode() = filter.hashCode()
    override fun toString() = filter.toString()

    fun toProto(
        filenames: List<String> = emptyList(),
        currentOffset: Int = 0,
    ): DynamicPlaylistProto =
        DynamicPlaylistProto
            .newBuilder()
            .setFilter(filter.toProto())
            .setShuffle(shuffle)
            .setCurrentOffset(currentOffset)
            .addAllFilenames(filenames)
            .build()
}
