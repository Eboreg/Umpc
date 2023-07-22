package us.huseli.umpc.mpd.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.umpc.mpd.MPDRepository

data class SnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onActionPerformed: (() -> Unit)? = null,
)

class MessageEngine(private val repo: MPDRepository) {
    private val _error = MutableStateFlow<SnackbarMessage?>(null)
    private val _message = MutableStateFlow<SnackbarMessage?>(null)

    val error = _error.asStateFlow()
    val message = _message.asStateFlow()

    fun setError(message: String?) {
        if (message != null) _error.value = SnackbarMessage(message)
        else _error.value = null
    }

    fun addMessage(message: String) {
        _message.value = SnackbarMessage(message)
    }

    fun addMessage(message: SnackbarMessage) {
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