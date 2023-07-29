package us.huseli.umpc.viewmodels

import androidx.annotation.IntRange
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.MPDStreamPlayer
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.SettingsRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class CurrentSongViewModel @Inject constructor(
    repo: MPDRepository,
    settingsRepository: SettingsRepository,
    val streamPlayer: MPDStreamPlayer,
) : BaseViewModel(repo) {
    val currentSongDuration = repo.currentSongDuration
    val currentSongElapsed = repo.currentSongElapsed
    val currentBitrate = repo.currentBitrate
    val currentAudioFormat = repo.currentAudioFormat
    val isDynamicPlaylistActive = repo.activeDynamicPlaylist.map { it != null }
    val isStreaming = streamPlayer.isStreaming
    val randomState = repo.randomState
    val repeatState = repo.repeatState
    val streamingUrl = settingsRepository.streamingUrl
    val stopAfterCurrent = repo.stopAfterCurrent

    init {
        repo.loadStatus()
        repo.loadActiveDynamicPlaylist(playOnLoad = false, replaceCurrentQueue = false)
    }

    fun deactivateDynamicPlaylist() = repo.deactivateDynamicPlaylist()

    fun next() = repo.next()

    fun previousOrRestart() = repo.previousOrRestart()

    fun setVolume(@IntRange(0, 100) value: Int) = repo.client.enqueue("setvol $value")

    fun seek(time: Double) = repo.seek(time)

    fun seekRelative(time: Double) {
        val timeString = if (time >= 0) "+$time" else time.toString()
        repo.client.enqueue("seekcur $timeString")
    }

    fun stop() = repo.stop()

    fun toggleRandomState() = repo.client.enqueue("random", if (repo.randomState.value) "0" else "1")

    fun toggleRepeatState() = repo.client.enqueue("repeat", if (repo.repeatState.value) "0" else "1")

    fun toggleStopAfterCurrent() = repo.toggleStopAfterCurrent()

    fun toggleStream(onFinish: ((Boolean) -> Unit)? = null) =
        viewModelScope.launch { onFinish?.invoke(streamPlayer.toggle()) }
}
