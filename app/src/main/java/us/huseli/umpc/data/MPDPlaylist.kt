package us.huseli.umpc.data

import us.huseli.umpc.toInstant
import java.time.Instant

data class MPDPlaylist(val name: String, val lastModified: Instant? = null) {
    override fun equals(other: Any?) = other is MPDPlaylist && other.name == name
    override fun hashCode() = name.hashCode()
}

fun Map<String, String>.toMPDPlaylist() = try {
    MPDPlaylist(
        name = this["playlist"]!!,
        lastModified = this["Last-Modified"]?.toInstant(),
    )
} catch (e: NullPointerException) {
    null
}
