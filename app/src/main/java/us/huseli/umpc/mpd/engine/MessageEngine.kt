package us.huseli.umpc.mpd.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.mpd.MPDRepository

class MessageEngine(private val repo: MPDRepository) {
    private val _error = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)

    val error = _error.asStateFlow()
    val message = _message.asStateFlow()

    fun setError(message: String?) {
        _error.value = message
    }

    fun addMessage(message: String) {
        _message.value = message
    }

    fun clearError() {
        _error.value = null
        repo.client.enqueue("clearerror")
    }

    fun clearMessage() {
        _message.value = null
    }
}