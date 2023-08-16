package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.annotation.IntRange
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.MPDStreamPlayer
import us.huseli.umpc.repository.AlbumArtRepository
import us.huseli.umpc.repository.DynamicPlaylistRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SettingsRepository
import us.huseli.umpc.viewmodels.abstr.AlbumArtViewModel
import javax.inject.Inject

@HiltViewModel
class CurrentSongViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    albumArtRepo: AlbumArtRepository,
    settingsRepository: SettingsRepository,
    private val dynamicPlaylistRepo: DynamicPlaylistRepository,
    private val streamPlayer: MPDStreamPlayer,
    @ApplicationContext context: Context,
) : AlbumArtViewModel(repo, messageRepo, albumArtRepo, context) {
    val currentSongDuration = repo.currentSongDuration
    val currentSongElapsed = repo.currentSongElapsed
    val currentBitrate = repo.currentBitrate
    val currentAudioFormat = repo.currentAudioFormat
    val isDynamicPlaylistActive = dynamicPlaylistRepo.activeDynamicPlaylist.map { it != null }
    val isStreaming = streamPlayer.isStreaming
    val randomState = repo.randomState
    val repeatState = repo.repeatState
    val streamingUrl = settingsRepository.streamingUrl
    val stopAfterCurrent = repo.stopAfterCurrent

    init {
        repo.loadStatus()
        dynamicPlaylistRepo.loadActiveDynamicPlaylist(playOnLoad = false, replaceCurrentQueue = false)
    }

    fun deactivateDynamicPlaylist() = dynamicPlaylistRepo.deactivateDynamicPlaylist()

    fun next() = repo.next()

    fun previousOrRestart() = repo.previousOrRestart()

    fun setVolume(@IntRange(0, 100) value: Int) = repo.setVolume(value)

    fun seek(time: Double) = repo.seek(time)

    fun seekRelative(time: Double) = repo.seekRelative(time)

    fun stop() = repo.stop()

    fun toggleRandomState() = repo.toggleRandomState()

    fun toggleRepeatState() = repo.toggleRepeatState()

    fun toggleStopAfterCurrent() = repo.toggleStopAfterCurrent()

    fun toggleStream(onFinish: ((Boolean) -> Unit)? = null) =
        viewModelScope.launch { onFinish?.invoke(streamPlayer.toggle()) }
}
