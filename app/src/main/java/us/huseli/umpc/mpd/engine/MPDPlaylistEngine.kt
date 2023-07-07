package us.huseli.umpc.mpd.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDPlaylistWithSongs
import us.huseli.umpc.mpd.response.MPDResponse
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener

class MPDPlaylistEngine(private val repo: MPDRepository) : OnMPDChangeListener {
    private val _playlistsWithSongs = MutableStateFlow<List<MPDPlaylistWithSongs>>(emptyList())
    private val _playlists = MutableStateFlow<List<MPDPlaylist>>(emptyList())

    val playlists = _playlists.asStateFlow()

    init {
        repo.registerOnMPDChangeListener(this)
    }

    fun deletePlaylist(playlistName: String, onFinish: (MPDResponse) -> Unit) =
        repo.client.enqueue("rm", playlistName, onFinish)

    fun enqueuePlaylistAndPlay(playlistName: String) {
        /** Clears the queue, loads playlist into it, and plays from position 0. */
        repo.client.enqueue("clear") {
            repo.client.enqueue("load", playlistName) {
                repo.client.enqueue("play 0")
            }
        }
    }

    private fun fetchPlaylists(onFinish: (List<MPDPlaylist>) -> Unit) {
        repo.client.enqueue("listplaylists") { response ->
            onFinish(response.extractPlaylists())
        }
    }

    fun fetchPlaylistSongs(playlistName: String, onFinish: (List<MPDSong>) -> Unit) {
        repo.client.enqueue("listplaylistinfo", playlistName) { response ->
            onFinish(response.extractSongs())
        }
    }

    fun loadPlaylists() {
        fetchPlaylists { _playlists.value = it }
    }

    private fun loadPlaylistsWithSongs() {
        fetchPlaylists { playlists ->
            playlists.forEach { playlist ->
                fetchPlaylistSongs(playlist.name) { songs ->
                    val pws = MPDPlaylistWithSongs(playlist, songs)

                    _playlistsWithSongs.value = _playlistsWithSongs.value
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

    fun renamePlaylist(name: String, newName: String, onFinish: (Boolean) -> Unit) {
        repo.client.enqueue("rename", listOf(name, newName)) { response ->
            onFinish(response.isSuccess)
        }
    }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) {
            loadPlaylists()
            loadPlaylistsWithSongs()
        }
    }
}
