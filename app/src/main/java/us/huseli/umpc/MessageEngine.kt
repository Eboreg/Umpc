package us.huseli.umpc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageEngine @Inject constructor() {
    private val _message = MutableStateFlow<String?>(null)

    val message = _message.asStateFlow()

    fun addMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }
}