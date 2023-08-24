package us.huseli.umpc.mpd

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDStreamPlayer @Inject constructor(
    private val settingsRepo: SettingsRepository,
    ioScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    private val _isStreaming = MutableStateFlow(false)
    private var exoPlayer: ExoPlayer? = null
    private var url: String? = null

    val isStreaming = _isStreaming.asStateFlow()

    init {
        ioScope.launch {
            settingsRepo.streamingUrl.collect { url = it }
        }
    }

    suspend fun toggle(): Boolean {
        return if (_isStreaming.value) {
            stop()
            false
        } else {
            start()
            true
        }
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun start() {
        url?.let {
            exoPlayer?.release()

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it))
                prepare()
                play()
                _isStreaming.value = true
            }
        }
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun stop() {
        exoPlayer?.apply {
            stop()
            release()
        }
        _isStreaming.value = false
        exoPlayer = null
    }
}
