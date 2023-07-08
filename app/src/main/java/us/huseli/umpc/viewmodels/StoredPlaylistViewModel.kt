package us.huseli.umpc.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.response.MPDResponse
import javax.inject.Inject

@HiltViewModel
class StoredPlaylistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel(repo) {
    private val playlistName: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())

    val songs = _songs.asStateFlow()
    val playlist = repo.engines.playlist.storedPlaylists.map { playlists -> playlists.find { it.name == playlistName } }

    init {
        repo.engines.playlist.fetchStoredPlaylistSongs(playlistName) { _songs.value = it }
    }

    fun deletePlaylist(onFinish: (MPDResponse) -> Unit) =
        repo.engines.playlist.deleteStoredPlaylist(playlistName, onFinish)

    fun play() = repo.engines.playlist.enqueueStoredPlaylistAndPlay(playlistName)

    fun rename(newName: String, onFinish: (Boolean) -> Unit) =
        repo.engines.playlist.renameStoredPlaylist(playlistName, newName, onFinish)
}
