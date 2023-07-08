package us.huseli.umpc.mpd.client

import kotlinx.coroutines.CoroutineScope
import us.huseli.umpc.Constants
import us.huseli.umpc.mpd.command.MPDBinaryCommand
import us.huseli.umpc.mpd.command.MPDCommand
import us.huseli.umpc.mpd.response.MPDResponse

class MPDBinaryClient(ioScope: CoroutineScope) : MPDClient(ioScope) {
    override suspend fun connect(failSilently: Boolean): Boolean {
        return if (super.connect(failSilently)) {
            socket.soTimeout = 500
            enqueue(MPDCommand("binarylimit ${Constants.BINARY_LIMIT}"))
            true
        } else false
    }

    override fun enqueue(
        command: String,
        args: Collection<String>,
        onFinish: ((MPDResponse) -> Unit)?
    ) = enqueue(MPDBinaryCommand(command, args, onFinish))
}
