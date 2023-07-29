package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDStreamPlayer
import us.huseli.umpc.mpd.response.MPDMapResponse
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MPDViewModel @Inject constructor(repo: MPDRepository, streamPlayer: MPDStreamPlayer) : BaseViewModel(repo) {
    private val _showVolumeFlash = MutableStateFlow(false)

    val error = repo.messageRepository.error
    val isStreaming = streamPlayer.isStreaming
    val loadingDynamicPlaylist = repo.loadingDynamicPlaylist
    val message = repo.messageRepository.message
    val showVolumeFlash = _showVolumeFlash.asStateFlow()

    fun addSongToStoredPlaylist(song: MPDSong, playlistName: String, onFinish: (MPDMapResponse) -> Unit) =
        repo.client.enqueue("playlistadd", listOf(playlistName, song.filename), onFinish)

    fun clearError() {
        repo.messageRepository.clearError()
        repo.client.enqueue("clearerror")
    }

    fun clearMessage() = repo.messageRepository.clearMessage()

    fun onVolumeDownPressed() {
        if (repo.volume.value > 0) {
            val volume = max(0, repo.volume.value - 5)
            _showVolumeFlash.value = true
            repo.client.enqueue("setvol $volume")
        }
    }

    fun onVolumeUpPressed() {
        if (repo.volume.value < 100) {
            val volume = min(100, repo.volume.value + 5)
            _showVolumeFlash.value = true
            repo.client.enqueue("setvol $volume")
        }
    }

    fun resetShowVolumeFlash() {
        _showVolumeFlash.value = false
    }
}
