package us.huseli.umpc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.MPDRepository
import java.io.FileFilter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: MPDRepository) : ViewModel() {
    val outputs = repo.outputs
    val hostname = repo.engines.settings.hostname.asStateFlow()
    val password = repo.engines.settings.password.asStateFlow()
    val port = repo.engines.settings.port.asStateFlow()
    val streamingUrl = repo.engines.settings.streamingUrl.asStateFlow()

    fun addMessage(message: String) = repo.engines.message.addMessage(message)

    fun clearAlbumArtCache(onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        repo.albumArtDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        repo.thumbnailDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        onFinish?.invoke()
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) {
        repo.engines.settings.setOutputEnabled(id, isEnabled)
        repo.engines.control.setOutputEnabled(id, isEnabled)
    }

    fun setHostname(value: String) {
        repo.engines.settings.hostname.value = value
    }

    fun setPassword(value: String) {
        repo.engines.settings.password.value = value
    }

    fun setPort(value: Int) {
        repo.engines.settings.port.value = value
    }

    fun setStreamingUrl(value: String) {
        repo.engines.settings.streamingUrl.value = value
    }

    fun save() = repo.engines.settings.save()
}
