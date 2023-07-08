package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.PlaylistType
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class PlaylistListViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    private val _displayType = MutableStateFlow(PlaylistType.STORED)

    val displayType = _displayType.asStateFlow()

    fun getStoredPlaylistSongCount(playlistName: String, onFinish: (Int) -> Unit) =
        repo.engines.playlist.fetchStoredPlaylistSongs(playlistName) { onFinish(it.size) }

    fun setDisplayType(value: PlaylistType) {
        _displayType.value = value
    }
}
