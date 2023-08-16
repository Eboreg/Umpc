package us.huseli.umpc.viewmodels

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDStreamPlayer
import us.huseli.umpc.mpd.response.MPDBatchMapResponse
import us.huseli.umpc.repository.DynamicPlaylistRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class MPDViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    dynamicPlaylistRepo: DynamicPlaylistRepository,
    streamPlayer: MPDStreamPlayer,
    @ApplicationContext context: Context,
) : BaseViewModel(repo, messageRepo, context) {
    private val _showVolumeFlash = MutableStateFlow(false)

    val error = messageRepo.error
    val isStreaming = streamPlayer.isStreaming
    val loadingDynamicPlaylist = dynamicPlaylistRepo.loadingDynamicPlaylist
    val message = messageRepo.message
    val showVolumeFlash = _showVolumeFlash.asStateFlow()

    fun addSongToStoredPlaylist(song: MPDSong, playlistName: String, onFinish: (MPDBatchMapResponse) -> Unit) =
        repo.addSongToPlaylist(song, playlistName, onFinish)

    fun clearError() {
        messageRepo.clearError()
        repo.clearError()
    }

    fun clearMessage() = messageRepo.clearMessage()

    fun onVolumeDownPressed() {
        if (repo.volume.value > 0) {
            val volume = max(0, repo.volume.value - 5)
            _showVolumeFlash.value = true
            repo.setVolume(volume)
        }
    }

    fun onVolumeUpPressed() {
        if (repo.volume.value < 100) {
            val volume = min(100, repo.volume.value + 5)
            _showVolumeFlash.value = true
            repo.setVolume(volume)
        }
    }

    fun resetShowVolumeFlash() {
        _showVolumeFlash.value = false
    }
}
