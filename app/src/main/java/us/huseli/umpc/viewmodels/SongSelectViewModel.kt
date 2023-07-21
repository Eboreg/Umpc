package us.huseli.umpc.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDBatchMapResponse

abstract class SongSelectViewModel(repo: MPDRepository) : BaseViewModel(repo) {
    private val _selectedSongs = MutableStateFlow<List<MPDSong>>(emptyList())
    val selectedSongs = _selectedSongs.asStateFlow()

    fun addSelectedSongsToPlaylist(playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.engines.playlist.addSongsToStoredPlaylist(_selectedSongs.value, playlistName, onFinish)

    fun deselectAllSongs() {
        _selectedSongs.value = emptyList()
    }

    fun enqueueSelectedSongs() = repo.engines.control.enqueueSongsLast(_selectedSongs.value)

    fun toggleSongSelected(song: MPDSong) {
        _selectedSongs.value = _selectedSongs.value.toMutableList().apply {
            if (contains(song)) remove(song)
            else add(song)
        }
    }
}
