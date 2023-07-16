package us.huseli.umpc.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.MPDRepository
import javax.inject.Inject

@HiltViewModel
class CurrentSongViewModel @Inject constructor(repo: MPDRepository) : BaseViewModel(repo) {
    val currentSongAlbumArt = repo.engines.image.currentSongAlbumArt.asStateFlow()
    val currentSongDuration = repo.currentSongDuration
    val currentSongElapsed = repo.currentSongElapsed
    val currentBitrate = repo.currentBitrate
    val currentAudioFormat = repo.currentAudioFormat
    val randomState = repo.randomState
    val repeatState = repo.repeatState
    val streamingUrl = repo.engines.settings.streamingUrl.asStateFlow()

    fun next() = repo.engines.control.next()
    fun playOrPause() = repo.engines.control.playOrPause()
    fun previousOrRestart() =
        if (currentSongElapsed.value?.takeIf { it > 2 } != null) seek(0.0)
        else repo.engines.control.previous()

    fun setVolume(value: Int) = repo.engines.control.setVolume(value)
    fun seek(time: Double) = repo.engines.control.seek(time)
    fun seekRelative(time: Double) = repo.engines.control.seekRelative(time)
    fun stop() = repo.engines.control.stop()
    fun toggleRandomState() = repo.engines.control.toggleRandomState()
    fun toggleRepeatState() = repo.engines.control.toggleRepeatState()
    fun toggleStream(onFinish: ((Boolean) -> Unit)? = null) =
        viewModelScope.launch { onFinish?.invoke(repo.streamPlayer.toggle()) }
}
