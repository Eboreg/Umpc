package us.huseli.umpc.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import us.huseli.umpc.proto.DynamicPlaylistProto

@Parcelize
data class DynamicPlaylist(
    val filter: DynamicPlaylistFilter,
    val server: MPDServer,
    val shuffle: Boolean = false,
    val songCount: Int? = null,
) : Parcelable {
    override fun equals(other: Any?) =
        other is DynamicPlaylist && other.filter == filter && other.shuffle == shuffle

    override fun hashCode() = filter.hashCode()
    override fun toString() = filter.toString()

    fun toProto(
        filenames: List<String> = emptyList(),
        currentOffset: Int = 0,
    ): DynamicPlaylistProto? = try {
        DynamicPlaylistProto.newBuilder().let {
            it.filter = filter.toProto()
            it.shuffle = shuffle
            it.currentOffset = currentOffset
            it.server = server.toString()
            it.clearFilenames()
            it.addAllFilenames(filenames)
            it.build()
        }
    } catch (e: Exception) {
        null
    }
}
