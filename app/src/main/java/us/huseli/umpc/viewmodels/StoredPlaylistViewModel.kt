package us.huseli.umpc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.mpd.response.MPDMapResponse
import javax.inject.Inject

@HiltViewModel
class StoredPlaylistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : SongSelectViewModel(repo), OnMPDChangeListener {
    private val playlistName: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    private val _removedSongs = mutableListOf<MPDSong>()
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())

    val listState = LazyListState()
    val songs = _songs.asStateFlow()
    val playlist = repo.engines.playlist.storedPlaylists.map { playlists -> playlists.find { it.name == playlistName } }

    init {
        loadSongs()
        repo.registerOnMPDChangeListener(this)
    }

    fun delete(onFinish: (MPDMapResponse) -> Unit) =
        repo.engines.playlist.deleteStoredPlaylist(playlistName, onFinish)

    fun enqueue(onFinish: (MPDMapResponse) -> Unit) =
        repo.engines.playlist.enqueueStoredPlaylist(playlistName, onFinish)

    fun moveSong(fromIdx: Int, toIdx: Int) =
        repo.engines.playlist.moveSongInStoredPlaylist(playlistName, fromIdx, toIdx)

    fun play() = repo.engines.playlist.playStoredPlaylist(playlistName)

    fun rename(newName: String, onFinish: (Boolean) -> Unit) =
        repo.engines.playlist.renameStoredPlaylist(playlistName, newName, onFinish)

    private fun loadSongs() = repo.engines.playlist.fetchStoredPlaylistSongs(playlistName) { _songs.value = it }

    fun removeSelectedSongs() {
        _removedSongs.clear()
        _removedSongs.addAll(selectedSongs.value)
        repo.engines.playlist.removeSongsFromStoredPlaylist(playlistName, selectedSongs.value) { deselectAllSongs() }
    }

    fun removeSong(song: MPDSong) {
        _removedSongs.clear()
        _removedSongs.add(song)
        repo.engines.playlist.removeSongFromStoredPlaylist(playlistName, song)
    }

    fun undoRemoveSongs() =
        repo.engines.playlist.addSongsWithPositionToStoredPlaylist(_removedSongs, playlistName) {
            _removedSongs.clear()
        }

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("stored_playlist")) loadSongs()
    }
}
