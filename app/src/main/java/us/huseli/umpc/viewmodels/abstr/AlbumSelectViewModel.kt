package us.huseli.umpc.viewmodels.abstr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository

abstract class AlbumSelectViewModel(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
) : AlbumArtViewModel(repo, messageRepo, albumArtRepo) {
    private val _selectedAlbums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    val selectedAlbums = _selectedAlbums.asStateFlow()

    fun addSelectedAlbumsToPlaylist(playlistName: String, onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.addAlbumsToPlaylist(_selectedAlbums.value, playlistName, onFinish)

    fun deselectAllAlbums() {
        _selectedAlbums.value = emptyList()
    }

    fun enqueueSelectedAlbums(onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.enqueueAlbumsLast(_selectedAlbums.value, onFinish)

    fun playSelectedAlbums() = repo.enqueueAlbumsNextAndPlay(_selectedAlbums.value)

    fun toggleAlbumSelected(album: MPDAlbum) {
        _selectedAlbums.value = _selectedAlbums.value.toMutableList().apply {
            if (contains(album)) remove(album)
            else add(album)
        }
    }
}
