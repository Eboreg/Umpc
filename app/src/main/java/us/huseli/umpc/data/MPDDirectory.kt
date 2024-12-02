package us.huseli.umpc.data

data class MPDDirectory(
    val path: String,
    val directories: List<MPDDirectory> = emptyList(),
    val songs: List<MPDSong> = emptyList(),
    val contentsLoaded: Boolean = false,
) {
    val name: String
        get() = path.substringAfterLast('/')

    fun isEmpty() = directories.isEmpty() && songs.isEmpty()
}
