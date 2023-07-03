package us.huseli.umpc.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.MPDRepository
import us.huseli.umpc.mpd.MPDStreamPlayer
import javax.inject.Inject

@HiltViewModel
class MPDViewModel @Inject constructor(
    repo: MPDRepository,
    private val streamPlayer: MPDStreamPlayer,
) : BaseViewModel(repo) {
    val error = repo.engines.message.error
    val isStreaming = streamPlayer.isStreaming
    val message = repo.engines.message.message
    val queue = repo.queue

    fun clearError() = repo.engines.message.clearError()
    fun clearMessage() = repo.engines.message.clearMessage()

    fun toggleStream(enabled: Boolean) = viewModelScope.launch {
        if (enabled) streamPlayer.start()
        else streamPlayer.stop()
    }
}
