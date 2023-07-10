package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.PlaylistType
import us.huseli.umpc.data.DynamicPlaylist
import us.huseli.umpc.data.DynamicPlaylistFilter
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    private val _displayType = MutableStateFlow(PlaylistType.STORED)

    val displayType = _displayType.asStateFlow()
    val dynamicPlaylists = repo.engines.playlist.dynamicPlaylists

    fun activateDynamicPlaylist(playlist: DynamicPlaylist) = repo.engines.playlist.activateDynamicPlaylist(playlist)

    fun createDynamicPlaylist(filter: DynamicPlaylistFilter, shuffle: Boolean) =
        repo.engines.playlist.createDynamicPlaylist(filter, shuffle)

    fun deleteDynamicPlaylist(playlist: DynamicPlaylist) = repo.engines.playlist.deleteDynamicPlaylist(playlist)

    fun getStoredPlaylistSongCount(playlistName: String, onFinish: (Int) -> Unit) =
        repo.engines.playlist.fetchStoredPlaylistSongs(playlistName) { onFinish(it.size) }

    fun setDisplayType(value: PlaylistType) {
        _displayType.value = value
    }

    fun updateDynamicPlaylist(
        playlist: DynamicPlaylist,
        filter: DynamicPlaylistFilter,
        shuffle: Boolean,
    ) = repo.engines.playlist.updateDynamicPlaylist(playlist, filter, shuffle)
}
