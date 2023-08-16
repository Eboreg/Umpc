package us.huseli.umpc.viewmodels

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import us.huseli.umpc.mpd.OnMPDChangeListener
import us.huseli.umpc.repository.MPDRepository
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SettingsRepository
import us.huseli.umpc.viewmodels.abstr.BaseViewModel
import java.io.FileFilter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    repo: MPDRepository,
    messageRepo: MessageRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext context: Context,
) : BaseViewModel(repo, messageRepo, context), OnMPDChangeListener {
    val outputs = repo.outputs
    val hostname = settingsRepository.hostname
    val password = settingsRepository.password
    val port = settingsRepository.port
    val streamingUrl = settingsRepository.streamingUrl

    init {
        repo.loadOutputs()
    }

    fun clearAlbumArtCache(onFinish: (() -> Unit)? = null) = viewModelScope.launch {
        albumArtDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        thumbnailDirectory.listFiles(FileFilter { it.isFile })?.forEach { it.delete() }
        onFinish?.invoke()
    }

    fun save() = settingsRepository.save()

    fun setHostname(value: String) = settingsRepository.setHostname(value)

    fun setOutputEnabled(id: Int, isEnabled: Boolean) = repo.setOutputEnabled(id, isEnabled)

    fun setPassword(value: String) = settingsRepository.setPassword(value)

    fun setPort(value: Int) = settingsRepository.setPort(value)

    fun setStreamingUrl(value: String) = settingsRepository.setStreamingUrl(value)

    override fun onMPDChanged(subsystems: List<String>) {
        if (subsystems.contains("output")) repo.loadOutputs()
    }
}
