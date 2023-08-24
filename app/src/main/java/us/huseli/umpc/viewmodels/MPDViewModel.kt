package us.huseli.umpc.viewmodels

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import us.huseli.umpc.LoggerInterface
import us.huseli.umpc.data.MPDSong
import us.huseli.umpc.mpd.MPDStreamPlayer
import us.huseli.umpc.mpd.response.MPDBatchTextResponse
import us.huseli.umpc.repository.DynamicPlaylistRepository
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SettingsRepository
import us.huseli.umpc.repository.SnackbarMessage
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
    settingsRepo: SettingsRepository,
) : BaseViewModel(repo, messageRepo), LoggerInterface {
    private val _showVolumeFlash = MutableStateFlow(false)
    private val isErrorSnackbarShown = MutableStateFlow(false)
    private val isMessageSnackbarShown = MutableStateFlow(false)

    val errorSnackbarHostState = SnackbarHostState()
    val isStreaming = streamPlayer.isStreaming
    val loadingDynamicPlaylist = dynamicPlaylistRepo.loadingDynamicPlaylist
    val messageSnackbarHostState = SnackbarHostState()
    val showVolumeFlash = _showVolumeFlash.asStateFlow()
    val servers = settingsRepo.servers

    init {
        collectSnackbarMessages(messageRepo.message, isMessageSnackbarShown, messageSnackbarHostState) {
            messageRepo.clearMessage()
        }
        collectSnackbarMessages(messageRepo.error, isErrorSnackbarShown, errorSnackbarHostState) {
            messageRepo.clearError()
            repo.clearError()
        }
    }

    inline fun addSongToStoredPlaylist(
        song: MPDSong,
        playlistName: String,
        crossinline onFinish: (MPDBatchTextResponse) -> Unit,
    ) = repo.addSongToPlaylist(song, playlistName, onFinish)

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

    private inline fun collectSnackbarMessages(
        messageFlow: Flow<SnackbarMessage?>,
        shownFlow: MutableStateFlow<Boolean>,
        hostState: SnackbarHostState,
        crossinline clearFunction: () -> Unit,
    ) = viewModelScope.launch {
        messageFlow
            .filterNotNull()
            .combine(shownFlow.filter { !it }) { m, _ -> m }
            .distinctUntilChanged()
            .collect { message ->
                shownFlow.value = true
                showSnackbarMessage(message, hostState)
                clearFunction()
                shownFlow.value = false
            }
    }

    private suspend fun showSnackbarMessage(message: SnackbarMessage, hostState: SnackbarHostState) {
        val result = hostState.showSnackbar(
            message = message.message,
            actionLabel = message.actionLabel,
            withDismissAction = true,
            duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) message.onActionPerformed?.invoke()
    }
}
