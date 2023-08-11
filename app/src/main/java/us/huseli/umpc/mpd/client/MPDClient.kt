package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.repository.MessageRepository
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
    private val messageRepository: MessageRepository,
) : MPDBaseClient(ioScope, settingsRepository) {
    private var firstConnect = true

    override suspend fun connect(failSilently: Boolean): Boolean {
        return super.connect(failSilently).also { result ->
            if (firstConnect) {
                if (result) messageRepository.addMessage("Connected to ${credentials!!.hostname}:${credentials!!.port}, protocol version ${protocolVersion.value}.")
                else messageRepository.addError("Failed to connect to ${credentials!!.hostname}:${credentials!!.port}.")
                firstConnect = false
            }
        }
    }
}
