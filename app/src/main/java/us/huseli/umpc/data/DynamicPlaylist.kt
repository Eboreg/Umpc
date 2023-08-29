package us.huseli.umpc.data

import kotlinx.parcelize.IgnoredOnParcel
import us.huseli.umpc.mpd.MPDFilter
import us.huseli.umpc.proto.DynamicPlaylistProto

data class DynamicPlaylist(
    val filters: List<DynamicPlaylistFilter>,
    val operator: Operator,
    val server: MPDServer,
    val shuffle: Boolean = false,
    val songCount: Int? = null,
) {
    enum class Operator(val display: String) { AND("and"), OR("or") }

    @IgnoredOnParcel
    val mpdFilters: List<MPDFilter>
        get() = filters.map { it.mpdFilter }

    fun toProto(
        filenames: List<String> = emptyList(),
        currentOffset: Int = 0,
    ): DynamicPlaylistProto? = try {
        DynamicPlaylistProto.newBuilder().let { proto ->
            proto.clearFilters()
            proto.addAllFilters(filters.map { it.toProto() })
            proto.operator = when (operator) {
                Operator.AND -> DynamicPlaylistProto.Operator.AND
                Operator.OR -> DynamicPlaylistProto.Operator.OR
            }
            proto.shuffle = shuffle
            proto.currentOffset = currentOffset
            proto.server = server.toString()
            proto.clearFilenames()
            proto.addAllFilenames(filenames)
            proto.build()
        }
    } catch (e: Exception) {
        null
    }

    override fun toString() = filters.joinToString(" ${operator.display} ")

    override fun equals(other: Any?) =
        other is DynamicPlaylist &&
        other.filters == filters &&
        other.shuffle == shuffle &&
        other.operator == operator &&
        other.server == server

    override fun hashCode(): Int {
        var result = filters.hashCode()
        result = 31 * result + operator.hashCode()
        result = 31 * result + server.hashCode()
        result = 31 * result + shuffle.hashCode()
        return result
    }
}
