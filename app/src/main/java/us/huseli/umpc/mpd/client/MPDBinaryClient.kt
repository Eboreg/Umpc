package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.Constants.BINARY_LIMIT
import us.huseli.umpc.mpd.command.MPDBinaryCommand
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.response.MPDBinaryResponse
import us.huseli.umpc.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MPDBinaryClient @Inject constructor(
    ioScope: CoroutineScope,
    settingsRepository: SettingsRepository,
) : MPDBaseClient(ioScope, settingsRepository) {
    override suspend fun connect(failSilently: Boolean): Boolean {
        return if (super.connect(failSilently)) {
            socket.soTimeout = 500
            enqueue(MPDCommand("binarylimit $BINARY_LIMIT"))
            true
        } else false
    }

    fun enqueue(command: String, arg: String, onFinish: ((MPDBinaryResponse) -> Unit)?) =
        enqueue(MPDBinaryCommand(command, listOf(arg), onFinish))
}
