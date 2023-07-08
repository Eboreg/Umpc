package us.huseli.umpc.mpd.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDPlaylistWithSongs
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDResponse

class MPDPlaylistEngine(private val repo: MPDRepository) : OnMPDChangeListener {
    private val _storedPlaylistsWithSongs = MutableStateFlow<List<MPDPlaylistWithSongs>>(emptyList())
    private val _storedPlaylists = MutableStateFlow<List<MPDPlaylist>>(emptyList())

    val storedPlaylists = _storedPlaylists.asStateFlow()

    init {
        repo.registerOnMPDChangeListener(this)
    }

    fun deleteStoredPlaylist(playlistName: String, onFinish: (MPDResponse) -> Unit) =
        repo.client.enqueue("rm", playlistName, onFinish)

    fun enqueueStoredPlaylistAndPlay(playlistName: String) {
        /** Clears the queue, loads playlist into it, and plays from position 0. */
        repo.client.enqueue("clear") {
            repo.client.enqueue("load", playlistName) {
                repo.client.enqueue("play 0")
            }
        }
    }

    private fun fetchStoredPlaylists(onFinish: (List<MPDPlaylist>) -> Unit) {
        repo.client.enqueue("listplaylists") { response ->
            onFinish(response.extractPlaylists())
        }
    }

    fun fetchStoredPlaylistSongs(playlistName: String, onFinish: (List<MPDSong>) -> Unit) {
        repo.client.enqueue("listplaylistinfo", playlistName) { response ->
            onFinish(response.extractSongs())
        }
    }

    fun loadStoredPlaylists() {
        fetchStoredPlaylists { _storedPlaylists.value = it }
    }

    private fun loadStoredPlaylistsWithSongs() {
        fetchStoredPlaylists { playlists ->
            playlists.forEach { playlist ->
                fetchStoredPlaylistSongs(playlist.name) { songs ->
                    val pws = MPDPlaylistWithSongs(playlist, songs)

                    _storedPlaylistsWithSongs.value = _storedPlaylistsWithSongs.value
                        .toMutableList()
                        .apply {
                            if (!contains(pws)) {
                                removeIf { it.playlist == playlist }
                                add(pws)
                            }
                        }.sortedByDescending { it.playlist.lastModified }
                }
            }
        }
    }

    fun renameStoredPlaylist(name: String, newName: String, onFinish: (Boolean) -> Unit) {
        repo.client.enqueue("rename", listOf(name, newName)) { response ->
            onFinish(response.isSuccess)
        }
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) {
            loadStoredPlaylists()
            loadStoredPlaylistsWithSongs()
        }
    }
}
