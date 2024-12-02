package us.huseli.umpc.data

import kotlinx.parcelize.IgnoredOnParcel
import us.huseli.umpc.toInstantOrNull
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class MPDPlaylist(
    val name: String,
    val lastModified: Instant? = null,
    val songs: List<MPDSong>? = null,
) {
    @IgnoredOnParcel
    val songCount: Int?
        get() = songs?.size

    val totalDuration: Duration?
        get() = songs
            ?.mapNotNull { it.duration }
            ?.sum()
            ?.roundToInt()
            ?.toDuration(DurationUnit.SECONDS)

    override fun equals(other: Any?) =
        other is MPDPlaylist && other.name == name && other.songs == songs

    override fun hashCode() = name.hashCode()
}

fun Map<String, String>.toMPDPlaylist() = try {
    MPDPlaylist(
        name = this["playlist"]!!,
        lastModified = this["last-modified"]?.toInstantOrNull(),
    )
} catch (e: NullPointerException) {
    null
}
