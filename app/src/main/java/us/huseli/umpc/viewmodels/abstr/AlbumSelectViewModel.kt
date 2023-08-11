package us.huseli.umpc.viewmodels.abstr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.mpd.command.MPDMapCommand
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.repository.MPDRepository

abstract class AlbumSelectViewModel(repo: MPDRepository) : BaseViewModel(repo) {
    private val _selectedAlbums = MutableStateFlow<List<MPDAlbum>>(emptyList())
    val selectedAlbums = _selectedAlbums.asStateFlow()

    fun addSelectedAlbumsToPlaylist(playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.client.enqueueBatch(
            commands = _selectedAlbums.value.map {
                MPDMapCommand(
                    "searchaddpl",
                    listOf(playlistName, it.getSearchFilter(repo.protocolVersion.value).toString())
                )
            },
            onFinish = onFinish,
        )

    fun deselectAllAlbums() {
        _selectedAlbums.value = emptyList()
    }

    fun enqueueSelectedAlbums(onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.client.enqueueBatch(
            commands = _selectedAlbums.value.map {
                MPDMapCommand(it.getSearchFilter(repo.protocolVersion.value).findadd())
            },
            onFinish = onFinish,
        )

    fun toggleAlbumSelected(album: MPDAlbum) {
        _selectedAlbums.value = _selectedAlbums.value.toMutableList().apply {
            if (contains(album)) remove(album)
            else add(album)
        }
    }
}
