package us.huseli.umpc.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onActionPerformed: (() -> Unit)? = null,
)

@Singleton
class MessageRepository @Inject constructor() {
    private val _error = MutableStateFlow<SnackbarMessage?>(null)
    private val _message = MutableStateFlow<SnackbarMessage?>(null)

    val error = _error.asStateFlow()
    val message = _message.asStateFlow()

    fun addError(message: String?) {
        if (message != null) _error.value = SnackbarMessage(message)
        else _error.value = null
    }

    fun addError(message: SnackbarMessage) {
        _error.value = message
    }

    fun addMessage(message: String) {
        _message.value = SnackbarMessage(message)
    }

    fun addMessage(message: SnackbarMessage) {
        _message.value = message
    }

    fun clearError() {
        _error.value = null
    }

    fun clearMessage() {
        _message.value = null
    }
}