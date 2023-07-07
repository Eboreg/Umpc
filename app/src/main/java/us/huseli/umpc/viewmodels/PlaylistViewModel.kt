package us.huseli.umpc.viewmodels

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import us.huseli.umpc.Constants.NAV_ARG_PLAYLIST
import us.huseli.umpc.data.MPDResponse
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    repo: MPDRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel(repo) {
    private val playlistName: String = savedStateHandle.get<String>(NAV_ARG_PLAYLIST)!!
    private val _songs = MutableStateFlow<List<MPDSong>>(emptyList())

    val songs = _songs.asStateFlow()
    val playlist = repo.engines.playlist.playlists.map { playlists -> playlists.find { it.name == playlistName } }

    init {
        repo.engines.playlist.fetchPlaylistSongs(playlistName) { _songs.value = it }
    }

    fun deletePlaylist(onFinish: (MPDResponse) -> Unit) =
        repo.engines.playlist.deletePlaylist(playlistName, onFinish)

    fun play() = repo.engines.playlist.enqueuePlaylistAndPlay(playlistName)

    fun rename(newName: String, onFinish: (Boolean) -> Unit) =
        repo.engines.playlist.renamePlaylist(playlistName, newName, onFinish)
}
