package us.huseli.umpc.mpd

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.Constants.PREF_STREAMING_URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDStreamPlayer @Inject constructor(@ApplicationContext private val context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var exoPlayer: ExoPlayer? = null
    private val _isStreaming = MutableStateFlow(false)
    private var url = preferences.getString(PREF_STREAMING_URL, "")

    val isStreaming = _isStreaming.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_STREAMING_URL) {
            url = preferences.getString(key, "")
        }
    }
}
