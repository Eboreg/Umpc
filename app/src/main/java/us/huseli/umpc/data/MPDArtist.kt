package us.huseli.umpc.data

import us.huseli.umpc.replaceLeadingJunk

open class MPDArtist(val name: String) {
    override fun equals(other: Any?) = other is MPDArtist && other.name == name
    override fun hashCode(): Int = name.hashCode()
    override fun toString() = "${javaClass.simpleName}[name: $name]"
}

class MPDArtistWithAlbums(name: String, val albums: List<MPDAlbum>) : MPDArtist(name) {
    override fun equals(other: Any?) =
        other is MPDArtistWithAlbums && super.equals(other) && other.albums == albums

    override fun hashCode(): Int = 31 * super.hashCode() + albums.hashCode()
}

fun <T : MPDArtist> Iterable<T>.sorted() = sortedBy { it.name.lowercase().replaceLeadingJunk() }
