package us.huseli.umpc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.SettingsRepository
import java.io.FileFilter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: MPDRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel(), OnMPDChangeListener {
    val outputs = repo.outputs
    val hostname = settingsRepository.hostname
    val password = settingsRepository.password
    val port = settingsRepository.port
    val streamingUrl = settingsRepository.streamingUrl

    init {
        repo.loadOutputs()
    }

    fun addMessage(message: String) = repo.messageRepository.addMessage(message)

    fun clearAlbumArtCache(onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        repo.albumArtDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        repo.thumbnailDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        onFinish?.invoke()
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) =
        repo.client.enqueue(if (isEnabled) "enableoutput $id" else "disableoutput $id")

    fun setHostname(value: String) = settingsRepository.setHostname(value)

    fun setPassword(value: String) = settingsRepository.setPassword(value)

    fun setPort(value: Int) = settingsRepository.setPort(value)

    fun setStreamingUrl(value: String) = settingsRepository.setStreamingUrl(value)

    fun save() = settingsRepository.save()

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("output")) repo.loadOutputs()
    }
}
