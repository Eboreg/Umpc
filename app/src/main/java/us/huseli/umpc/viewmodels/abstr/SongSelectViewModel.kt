package us.huseli.umpc.viewmodels.abstr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.repository.MPDRepository

abstract class SongSelectViewModel(repo: MPDRepository) : BaseViewModel(repo) {
    private val _selectedSongs = MutableStateFlow<List<MPDSong>>(emptyList())
    val selectedSongs = _selectedSongs.asStateFlow()

    fun addSelectedSongsToPlaylist(playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.client.enqueueBatch(
            commands = _selectedSongs.value.map { MPDMapCommand("playlistadd", listOf(playlistName, it.filename)) },
            onFinish = onFinish,
        )

    fun deselectAllSongs() {
        _selectedSongs.value = emptyList()
    }

    fun enqueueSelectedSongs(onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.client.enqueueBatch(_selectedSongs.value.map { MPDMapCommand("add", it.filename) }, onFinish = onFinish)

    fun toggleSongSelected(song: MPDSong) {
        _selectedSongs.value = _selectedSongs.value.toMutableList().apply {
            if (contains(song)) remove(song)
            else add(song)
        }
    }
}
