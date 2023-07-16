package us.huseli.umpc.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class MPDViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    private val _showVolumeFlash = MutableStateFlow(false)

    val error = repo.engines.message.error
    val message = repo.engines.message.message
    val showVolumeFlash = _showVolumeFlash.asStateFlow()
    val loadingDynamicPlaylist = repo.engines.playlist.loadingDynamicPlaylist

    fun clearError() = repo.engines.message.clearError()
    fun clearMessage() = repo.engines.message.clearMessage()

    fun onVolumeUpPressed() {
        _showVolumeFlash.value = true
        repo.engines.control.volumeUp()
    }

    fun onVolumeDownPressed() {
        _showVolumeFlash.value = true
        repo.engines.control.volumeDown()
    }

    fun resetShowVolumeFlash() {
        _showVolumeFlash.value = false
    }
}
