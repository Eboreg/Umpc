package us.huseli.umpc.viewmodels.abstr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import us.huseli.umpc.data.MPDAlbum
import us.huseli.umpc.data.MPDPlaylist
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SnackbarMessage

abstract class BaseViewModel(
    protected val repo: MPDRepository,
    protected val messageRepo: MessageRepository,
) : ViewModel() {
    private val _storedPlaylists = MutableStateFlow<List<MPDPlaylist>>(emptyList())

    val connectedServer = repo.connectedServer
    val currentSong = repo.currentSong
    val isConnected = repo.isConnected
    val isIOError = repo.isIOError
    val playerState = repo.playerState
    val storedPlaylists = _storedPlaylists.asStateFlow()
    val volume = repo.volume

    init {
        viewModelScope.launch {
            repo.playlists.filterNotNull().distinctUntilChanged().collect {
                _storedPlaylists.value = it
            }
        }

        viewModelScope.launch {
            repo.error.collect { error ->
                if (error != null) messageRepo.addError(error)
            }
        }
    }

    fun addError(message: String) = messageRepo.addError(message)

    fun addError(message: SnackbarMessage) = messageRepo.addError(message)

    fun addMessage(message: String) = messageRepo.addMessage(message)

    fun addMessage(message: SnackbarMessage) = messageRepo.addMessage(message)

    fun enqueueAlbumLast(album: MPDAlbum, onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.enqueueAlbumLast(album, onFinish)

    fun enqueueSongLast(song: MPDSong, onFinish: (MPDBatchTextResponse) -> Unit) =
        repo.enqueueSongLast(song, onFinish)

    fun playAlbum(album: MPDAlbum?) = album?.let { repo.enqueueAlbumNextAndPlay(album) }

    fun playOrPause() = repo.playOrPause()

    fun playOrPauseSong(song: MPDSong) {
        if (song.id != null) {
            if (song.id == repo.currentSongId.value) playOrPause()
            else repo.playSongById(song.id)
        } else repo.enqueueSongNextAndPlay(song)
    }

    fun playSongByPosition(pos: Int) = repo.playSongByPosition(pos)
}
