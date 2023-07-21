package us.huseli.umpc.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDBatchMapResponse

abstract class AlbumSelectViewModel(repo: MPDRepository) : BaseViewModel(repo) {
    private val _selectedAlbums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    val selectedAlbums = _selectedAlbums.asStateFlow()

    fun addSelectedAlbumsToPlaylist(playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.engines.playlist.addAlbumsToStoredPlaylist(_selectedAlbums.value, playlistName, onFinish)

    fun deselectAllAlbums() {
        _selectedAlbums.value = emptyList()
    }

    fun enqueueSelectedAlbums() {
        _selectedAlbums.value.forEach { album ->
            repo.engines.control.enqueueAlbumLast(album) { response ->
                if (!response.isSuccess) repo.engines.message.addMessage("Could not enqeue album: ${response.error}")
            }
        }
    }

    fun toggleAlbumSelected(album: MPDAlbum) {
        _selectedAlbums.value = _selectedAlbums.value.toMutableList().apply {
            if (contains(album)) remove(album)
            else add(album)
        }
    }
}