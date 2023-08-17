package us.huseli.umpc.viewmodels.abstr

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.mpd.response.MPDTextResponse
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository

abstract class AlbumSelectViewModel(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    context: Context
) : AlbumArtViewModel(repo, messageRepo, albumArtRepo, context) {
    private val _selectedAlbums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    val selectedAlbums = _selectedAlbums.asStateFlow()

    fun addSelectedAlbumsToPlaylist(playlistName: String, onFinish: (MPDTextResponse) -> Unit) =
        repo.addAlbumsToPlaylist(_selectedAlbums.value, playlistName, onFinish)

    fun deselectAllAlbums() {
        _selectedAlbums.value = emptyList()
    }

    fun enqueueSelectedAlbums(onFinish: (MPDTextResponse) -> Unit) =
        repo.enqueueAlbumsLast(_selectedAlbums.value, onFinish)

    fun playSelectedAlbums() = repo.enqueueAlbumsNextAndPlay(_selectedAlbums.value)

    fun toggleAlbumSelected(album: MPDAlbum) {
        _selectedAlbums.value = _selectedAlbums.value.toMutableList().apply {
            if (contains(album)) remove(album)
            else add(album)
        }
    }
}
