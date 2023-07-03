package us.huseli.umpc.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.MessageEngine
import us.huseli.umpc.SettingsRepository
import us.huseli.umpc.mpd.MPDEngine
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val engine: MPDEngine,
    private val messageEngine: MessageEngine,
    private val repository: SettingsRepository,
) : ViewModel() {
    val outputs = engine.outputs
    val hostname = repository.hostname.asStateFlow()
    val password = repository.password.asStateFlow()
    val port = repository.port.asStateFlow()
    val streamingUrl = repository.streamingUrl.asStateFlow()

    init {
        engine.fetchOutputs()
    }

    fun setOutputEnabled(id: Int, isEnabled: Boolean) {
        repository.setOutputEnabled(id, isEnabled)
        engine.setOutputEnabled(id, isEnabled)
    }

    fun setHostname(value: String) {
        repository.hostname.value = value
    }

    fun setPassword(value: String) {
        repository.password.value = value
    }

    fun setPort(value: Int) {
        repository.port.value = value
    }

    fun setStreamingUrl(value: String) {
        repository.streamingUrl.value = value
    }

    fun save() {
        repository.save()
        messageEngine.addMessage("Settings saved.")
    }
}
